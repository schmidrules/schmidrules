
/**
 * @addtogroup BlacklistManager
 * Blacklisting for MSISDN
 */
/** @{ */
/**
 * @file
 * BlacklistManager for MSISDN
 *
 * @copydoc transact::blacklist::BlacklistManager
 * @copydoc Blacklist
 * @author DI Andreas Welzl (http://xion.at)
 * @version $Header$
 */
/* ----------------------------------------------------------------- */

#include "config/entity/Country.h"
#include "dgw/psms.h"
#include "dgwlib/log.h"
#include "transact/blacklist/BlacklistManager.h"
#include "transact/message/generate.h"
#include "transact/msisdn/MsisdnManager.h"
#include "util/dbOracle.h"
#include "util/timing.h"

#define BLACKLIST_STATE_HARD         -1
#define BLACKLIST_STATE_NONE          0
#define BLACKLIST_STATE_SOFT_VALID    1

static LinkedList_Iterator* blacklistManager_createBlacklistIterator(LinkedList* resL, long msisdn_id, const char* log_info)
{
  dgw_assert(NULL != log_info);
  if (0 == msisdn_id)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE, "blacklistManager_createBlacklistIterator called with msisdn_id 0!");
  }

  if (resL != NULL)
  {
    const long itemCount = linkedList_itemCount(resL);

    dgw_info("%s: %ld blacklist entries for msisdn[id:%ld]", log_info, itemCount, msisdn_id);
    if (0 == itemCount)
    {
      return NULL;
    }
    else
    {
      return linkedList_Iterator_create(resL, COLLECTION_LOCK_READ);
    }
  }

  return NULL;
}

#define blacklistManager_blacklistingEnabled() \
  blacklistManager_blacklistingEnabled_impl(__func__)
static bool blacklistManager_blacklistingEnabled_impl(const char* func)
{
  if (!blacklistEB_blacklistingEnabled())
  {
    dgw_info("%s: Blacklisting disabled on this instance!", func);
    return false;
  }
  return true;
}

static bool blacklistManager_ignore_blacklist_entry(const message* msg,
    BlacklistType listing_type,
    BlacklistReason reason)
{

  // the listing type check
  if ((listing_type == BLACKLIST_TMP_REJ) ||
      (listing_type == BLACKLIST_TMP_REJ_RETRY))
  {
    return true;
  }

  if (reason == BL_REASON_TERMINATION_KEYWORD)
  {
    // ignore stop blacklist check
    if (msg->app->ignore_stop_blacklist)
    {
      return true;
    }

    if ((msg->action == ACTION_BYPASS_TERMINATION_BL) &&
        message_is_free(msg))
    {
      return true;
    }
  }

  // check blacklist reason and message tariff
  if ((reason == BL_REASON_24H_FIXED_SOFT) &&
      message_is_free(msg))
  {
    return true;
  }

  return false;
}

static long process_entry_hard(BlacklistEB* entry,
                               long blacklist_state,
                               bool* remove)
{
  dgw_assert(blacklistEB_getListingType(entry) == BLACKLIST_HARD);

  if (blacklistEB_isValid(blacklistEB_getValidTo(entry)))
  {
    blacklist_state = BLACKLIST_STATE_HARD;
  }
  else
  {
    *remove = true;
  }

  dgw_trace("%s returns black list state[%ld]", __func__, blacklist_state);

  return blacklist_state;
}

// void * -> message*
static long process_entry_soft(BlacklistEB* entry, long blacklist_state, void* data, bool* remove)
{
  // handling of soft blacklist, no hard blacklist yet
  if (blacklist_state >= BLACKLIST_STATE_NONE)
  {
    const message* msg = data;
    const BlacklistReason reason = blacklistEB_getReason(entry);
    if (msg->dialogMsgType != PSMS_MSG_STOP_RECEIVED &&
        reason != BL_REASON_24H_FIXED_SOFT)
    {
      BlacklistType listingType = blacklistEB_getListingType(entry);
      // PSMS-94: do not allow MO generator to remove every soft blacklist
      if ((msg->handset != HANDSET_TYPE_MO_GENERATOR) // if is real MO: remove blacklist
          || ((listingType == BLACKLIST_SOFT) // if generated MO only remove soft blacklist entry with following reasons:
              && (reason <= BL_REASON_CUSTOM // - no reason definied (NULL or 0), hardly actual
                  || reason == BL_REASON_TERMINATION_KEYWORD // - classic STOP blacklist
                  || reason == BL_REASON_SMSC_REJECTED // - autoblacklist on SMSC response
                  || reason == BL_REASON_TMP_REJ_TO_SOFT // - CH_SWISSCOM: pre paid customer without balance
                  || reason == BL_REASON_CUSTOMER_MANUAL // - customer complain; hopefully this will be removed later
                  || reason == BL_REASON_OPERATOR_MANUAL // - customer complain; hopefully this will be removed later
                  || reason == BL_REASON_FROM_TOOL // - entry from deamon tool
                  || (reason == BL_REASON_LIMIT_REACHED // - billing interrupt or termination reached longer than 14 days ago
                      && blacklistEB_getCreationTime(entry)->tv_sec + TIMING_TWOWEEKS < time(NULL)))))
      {
        dgw_trace("blacklist entry[%ld] is marked for deletion", blacklistEB_getId(entry));
        *remove = true;
      }
      else
      {
        blacklist_state = BLACKLIST_STATE_HARD;
      }
      dgw_trace("msisdn[%s] black list state[%ld]", msg->msisdn, blacklist_state);
    }
    else
    {
      *remove = false;
      dgw_debug("msisdn[%s] black list state NOT changed due to dialog type[%d], blacklist reason[%d]",
                msg->msisdn, msg->dialogMsgType, reason);
    }
  }
  dgw_info("%s returns black list state[%ld] for msisdn[%s]", __func__, blacklist_state, ((message*)data)->msisdn);
  return blacklist_state;
}

static long process_blacklist_entry(BlacklistEB* entry, int blacklist_state, void* data, bool* remove)
{

  BlacklistType listing_type = blacklistEB_getListingType(entry);
  if (listing_type == BLACKLIST_MAX_AQM)
  {
    *remove = false;
    return blacklist_state;
  }
  else if (listing_type == BLACKLIST_HARD)
  {
    blacklist_state = process_entry_hard(entry,
                                         blacklist_state,
                                         remove);
  }

  blacklist_state = process_entry_soft(entry, blacklist_state, data, remove);

  return blacklist_state;
}
/*
 * true msisdn is blacklisted
 * false msisdn is not blacklisted
 * used only once system_incoming_queue
 */

bool blacklistManager_is_blacklisted_on_mo(persistence_manager* pmgr, message* msg)
{
  dgw_assert(msg != NULL);
  dgw_assert(msg->svc_no != NULL);

  if (!blacklistManager_blacklistingEnabled())
  {
    return false;
  }

  LinkedList* res = blacklistEB_find(pmgr, msg->msisdn_id, msg->svc_no->id, msg->app_id);
  LinkedList_Iterator* iter = blacklistManager_createBlacklistIterator(res, msg->msisdn_id, "check blacklist for MO");

  if (iter == NULL)
  {
    FREE_AND_NULL(res, linkedList_destroy);
    dgw_debug("no black list entries found");
    return false;
  }

  dgw_debug("checking black list for msisdn[%s] svcid[%ld] appid[%ld] for MO", msg->msisdn, msg->svc_no->id, msg->app_id);
  int blacklist_state = BLACKLIST_STATE_NONE;

  size_t removed_blacklistentries = 0;

  for (BlacklistEB* entry = linkedList_Iterator_next(iter); entry != NULL; entry = linkedList_Iterator_next(iter))
  {
    bool remove_entry = false;
    dgw_trace("process blacklist entry with id %ld", blacklistEB_getId(entry));
    blacklist_state = process_blacklist_entry(entry, blacklist_state, msg, &remove_entry);
    if (remove_entry)
    {
      int rc = blacklistEB_delete(entry, pmgr);
      if (rc > 0)
      {
        removed_blacklistentries++;
      }
    }
    FREE_AND_NULL(entry, blacklistEB_destroy);
  }

  linkedList_Iterator_destroy(iter);
  linkedList_destroy(res);

  dgw_debug("%s calculated blacklist state[%d]", __func__, blacklist_state);

  if (removed_blacklistentries > 0)
  {
    dgw_info("%s has deleted [%ld] blacklist entries", __func__, removed_blacklistentries);
  }

  return (blacklist_state < BLACKLIST_STATE_NONE);
}

/*
 * used twice application listener, retry
 * document psms_code
 */
bool blacklistManager_is_blacklisted_on_mt(persistence_manager* pmgr,
    message* msg,
    PsmsReturnCode* psms_code,
    long* valid_end)
{
  // msg is assert
  if (msg == NULL || !blacklistManager_blacklistingEnabled())
  {
    return false;
  }

  const long servicenumber_id = (msg->svc_no == NULL) ? 0 : msg->svc_no->id;

  /** @todo check if necessary to retrieve the records or return the whole list. */
  /* answer to odot: at the moment it is necessary to do that because out dated entries are deleted */
  LinkedList* res = blacklistEB_find(pmgr, msg->msisdn_id, servicenumber_id, msg->app_id);
  LinkedList_Iterator* resIter = blacklistManager_createBlacklistIterator(res, msg->msisdn_id, "check blacklist for MT");

  if (resIter == NULL)
  {
    FREE_AND_NULL(res, linkedList_destroy);
    dgw_debug("no black list entries found");
    return false;
  }

  dgw_debug("checking black list for msisdn[%s] svcid[%ld] appid[%ld] for MT", msg->msisdn, servicenumber_id, msg->app_id);
  long blState = BLACKLIST_STATE_NONE;
  int reasondetail[BLACKLIST_REASON_COUNT + 2] = {0};
  BlacklistEB* blEB;
  PsmsReturnCode local_psms_code = PSMS_RC_ACCEPTED;
  long local_valid_to = -1; // not set

  while ((blEB = linkedList_Iterator_next(resIter)) != NULL)
  {
    if (blacklistEB_getListingType(blEB) == BLACKLIST_HARD)
    {
      // expired temporary hard blacklists are not removed by MT
      // they are like soft blacklists
      blState = BLACKLIST_STATE_HARD;
      reasondetail[0]++;   // [0] indicates hardblacklist
      local_psms_code = PSMS_RC_BLACKLISTED;
      local_valid_to = blacklistEB_getValidTo(blEB);
    }

    // handling of soft blacklist, no hard blacklist yet
    if (blState >= BLACKLIST_STATE_NONE)
    {
      BlacklistReason reason = blacklistEB_getReason(blEB);

      bool ignoreBL = blacklistManager_ignore_blacklist_entry(msg,
                      blacklistEB_getListingType(blEB),
                      reason);
      dgw_trace("ignore blacklist entry (result: %d), (apptype %d) (listingtype %d) (reason %d), (ignore_stop_blacklist (%d)",
                ignoreBL, msg->app->appType, blacklistEB_getListingType(blEB), reason, msg->app->ignore_stop_blacklist);

      if (!ignoreBL)
      {
        if (blacklistEB_isValidBlacklist(blEB))
        {
          //unlimited or not yet expired blacklist
          if (reason < BLACKLIST_REASON_COUNT)
          {
            reasondetail[reason + 2]++; //reason start with -1
          }
          //permanent blacklisting: further temporary soft blacklists expiration timestamps are irrelevant
          if (blacklistEB_isPermanentBlacklist(blEB))
          {
            local_psms_code = PSMS_RC_BLACKLISTED;
            blState = BLACKLIST_STATE_SOFT_VALID;
            local_valid_to = blacklistEB_getValidTo(blEB);
          }

          //blState != BLACKLIST_STATE_SOFT_VALID: do not overwrite information about permanent blacklists
          if (blState != BLACKLIST_STATE_SOFT_VALID && blState < blacklistEB_getValidTo(blEB))
          {
            blState = blacklistEB_getValidTo(blEB);
            local_valid_to = blState;
            local_psms_code = PSMS_RC_TMP_BLACKLISTED;
          }
        }
        else
        {
          /* blacklist expired, remove now */
          blacklistEB_delete(blEB, pmgr);
        }
      }
    }

    FREE_AND_NULL(blEB, blacklistEB_destroy);
  }

  if (resIter != NULL)
  {
    linkedList_Iterator_destroy(resIter);
  }
  if (res != NULL)
  {
    linkedList_destroy(res);
  }

  if (blState != BLACKLIST_STATE_HARD &&
      reasondetail[2 + BL_REASON_LIMIT_REACHED] > 0)
  {
    local_psms_code = PSMS_RC_LIMIT_REACHED;
  }

  bool is_blacklisted = blState != BLACKLIST_STATE_NONE;
  if (is_blacklisted)
  {
    if (psms_code != NULL)
    {
      *psms_code = local_psms_code;
    }
    if (valid_end != NULL)
    {
      *valid_end = local_valid_to;
    }
  }
  dgw_debug("%s determines black list state[%ld] psms code[%d] valid_to[%ld] for msisdn[%s]",
            __func__, blState, local_psms_code, local_valid_to, msg->msisdn);
  return is_blacklisted;
}


static bool check_blacklisting_done(long msisdn_id, long svcnumid, long appid, long valid_to)
{
  if (msisdn_id <= 0 && svcnumid <= 0 && appid <= 0)
  {
    /* nothing defined - entry would blacklist ALL - not allowed!!! */
    dgw_error(DGW_BAD_PARAMETER_SET,
              "illegal call, msisdn_id[%ld], svcnum_id[%ld] and app_id[%ld] not defined",
              msisdn_id, svcnumid, appid);
    return false;
  }
  if (!blacklistEB_isValid(valid_to))
  {
    dgw_warn("blacklist not done - valid_to[%ld] passed", valid_to);
    return false;
  }

  return true;
}

void blacklistManager_blacklist_msisdn(persistence_manager* pmgr,
                                       char* msisdn_num,
                                       long msisdn_id,
                                       long svcnumid,
                                       long appid,
                                       BlacklistType type,
                                       BlacklistReason reason,
                                       const char* description,
                                       long valid_to)
{
  if (!blacklistManager_blacklistingEnabled())
  {
    return;
  }

  dgw_debug("try to blacklist msisdn_id[%ld] svcnumid[%ld], appid[%ld] "
            "listing type[%d] reason[%d] description[%s] valid to[%ld]",
            msisdn_id, svcnumid, appid, type, reason,
            description, valid_to);

  if (msisdn_id == MSISDN_ID_NOTSET)
  {
    //this shall not be part of the function
    msisdn_id = msisdnManager_get_msisdnid_by_num(pmgr, msisdn_num);
    dgw_trace("we need msisdn id(%ld) for msisdn(%s)", msisdn_id, msisdn_num);
  }

  if (!check_blacklisting_done(msisdn_id, svcnumid, appid, valid_to))
  {
    return;
  }

  bool entry_exist = blacklist_exist_entry(pmgr, msisdn_id,
                     svcnumid, appid, type, valid_to);

  if (entry_exist)
  {
    dgw_info("blacklist not done, entry"
             " (msisdnid: %ld, svcnumid %ld, appid %ld, listing_type %d, valid_to %ld) already exits",
             msisdn_id, svcnumid, appid, type, valid_to);
    return;
  }
  // create entry and persist
  Octstr* odesc = NULL;
  if (description != NULL)
  {
    odesc = octstr_create(description);
  }
  BlacklistEB* entry = blacklistEB_create(msisdn_id, svcnumid, appid, type, reason,
                                          odesc, valid_to);

  blacklistEB_persist(entry, pmgr);

  FREE_AND_NULL(entry, blacklistEB_destroy);
}

static void get_delete_sql_bind(const long msisdn_id,
                                const long svcnum_id,
                                const long app_id,
                                const BlacklistType listing_type,
                                char  sql_buf[],
                                const size_t buf_size,
                                List* binds)
{
  /*
   * SQL delet statement for blacklist
   */
  str_cpy(sql_buf, buf_size, "DELETE FROM blacklist WHERE msisdn_id = :1 ");
  gwlist_append(binds, octstr_format("%ld", msisdn_id));

  unsigned int index = 2;

  str_format_append(sql_buf, buf_size, "AND (listing_type = :%d) ", index++);
  gwlist_append(binds, octstr_format("%d", listing_type));

  if (svcnum_id == 0)
  {
    str_cat(sql_buf, buf_size, "AND (service_number_id IS NULL OR service_number_id = 0) ");
  }
  else
  {
    str_format_append(sql_buf, buf_size, "AND (service_number_id = :%d) ", index++);
    gwlist_append(binds, octstr_format("%ld", svcnum_id));
  }
  if (app_id == 0)
  {
    str_cat(sql_buf, buf_size, "AND (application_id IS NULL OR application_id = 0)");
  }
  else
  {
    str_format_append(sql_buf, buf_size, "AND (application_id = :%d) ", index++);
    gwlist_append(binds, octstr_format("%ld", app_id));
  }
}

int blacklistManager_revoke_blacklisting(persistence_manager* pmgr,
    long msisdn_id,
    long svcnum_id,
    long app_id,
    BlacklistType listing_type)
{
  if (!blacklistManager_blacklistingEnabled())
  {
    dgw_warn("'%s' is not executed, blacklisting is DISABLED", __func__);
    return 0;
  }

  char sql_buf[1024];
  List* binds = gwlist_create();

  get_delete_sql_bind(msisdn_id, svcnum_id, app_id, listing_type, sql_buf, DIM(sql_buf), binds);

  Octstr* osql = octstr_create(sql_buf);

  int num_deleted = dbOracle_update(pmgr, osql, binds, NULL, NULL);
  gwlist_destroy(binds, octstr_destroy_item);
  octstr_destroy(osql);

  return num_deleted;
}

bool blacklistManager_is_msisdn_blacklisted(persistence_manager* pmgr,
    const char* msisdn_num,
    const long msisdn_id,
    long svcnum_id,
    long app_id,
    BlacklistType type,
    long expiration_period, // period (relative duration)
    time_t* creation_date)
{
  if (!blacklistManager_blacklistingEnabled())
  {
    return false;
  }

  LinkedList* res = blacklistEB_select_on_listingtype(pmgr, msisdn_id, type, svcnum_id, app_id);
  LinkedList_Iterator* iter = blacklistManager_createBlacklistIterator(res, msisdn_id, __func__);

  if (iter == NULL)
  {
    FREE_AND_NULL(res, linkedList_destroy);
    return false;
  }
  dgw_trace("%s checks %ld blacklist entries using msisdn[%s] svcid[%ld] appid[%ld] type[%d] period[%ld]",
            __func__,
            linkedList_itemCount(res),
            msisdn_num,
            svcnum_id,
            app_id,
            type,
            expiration_period);


  BlacklistEB* entry = NULL;
  unsigned blacklist_counter = 0;
  unsigned removed_blacklist_counter = 0;
  long     blacklistentry_mostrecent_creationdate = 0;

  while ((entry = linkedList_Iterator_next(iter)) != NULL)
  {
    if (blacklistEB_isExpired(entry, expiration_period))
    {
      // NOTE: overwrite the value if creation date is already set by other entries
      dgw_trace("expired blacklist entry [%ld], creation_date [%ld], valid_to [%ld]",
                blacklistEB_getId(entry),
                blacklistEB_getCreationDate(entry),
                blacklistEB_getValidTo(entry));
      ++removed_blacklist_counter;
      blacklistEB_delete(entry, pmgr);
    }
    else
    {
      // still valid blacklist entry found
      if (blacklistEB_getCreationDate(entry) > blacklistentry_mostrecent_creationdate)
      {
        // take the most recent blacklist entry
        dgw_debug("creation date is taken from valid entry[%ld]", blacklistEB_getId(entry));
        blacklistentry_mostrecent_creationdate = blacklistEB_getCreationDate(entry);
      }
      /* indicates that the msisdn is blacklisted by listing type*/
      blacklist_counter++;
    }
    FREE_AND_NULL(entry, blacklistEB_destroy);
  }
  linkedList_Iterator_destroy(iter);
  linkedList_destroy(res);

  if (creation_date != NULL)
  {
    dgw_debug("returned creation date is %ld", blacklistentry_mostrecent_creationdate);
    *creation_date = blacklistentry_mostrecent_creationdate;
  }
  dgw_info("msisdn[%s] is%s blacklisted by blacklist entries[%d], but removed[%d] for listing type[%d]",
           msisdn_num,
           (blacklist_counter > 0) ? "" : " NOT",
           blacklist_counter,
           removed_blacklist_counter,
           type);
  return (blacklist_counter > 0);
}

/* ----------------------------------------------------------------- */
/** @} */
