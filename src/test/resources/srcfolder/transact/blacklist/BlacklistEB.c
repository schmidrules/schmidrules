
/**
 * @addtogroup blacklistEB
 * Blacklisting for MSISDN
 */
/** @{ */
/**
 * @file
 * Blacklisting for MSISDN
 *
 * @copydoc transact::blacklist::Blacklist
 * @copydoc Blacklist
 * @author DI Andreas Welzl (http://xion.at)
 * @version $Header$
 */
/* ----------------------------------------------------------------- */

#include "dgw/psms.h"
#include "transact/blacklist/BlacklistEB.h"
#include "util/dbOracle.h"

/**
 * Enumeration of Blacklist fields
 *
 * @note /b Important: BLACKLIST_VARIABLE_FIELDCOUNT must be the last entry.
 *       Therefore add new entries before and do not delete it!
 */
enum
{
  BL_id,              /**< Id of the blacklist item */
  BL_applicationId,   /**< the blacklisted application */
  BL_keywordId,       /**< the blacklisted keyword (not supported now) */
  BL_serviceNumberId, /**< the blacklisted service number */
  BL_msisdnId,        /**< the blacklisted MSISDN */
  BL_listingType,     /**< hard/soft blacklisting */
  BL_reason,          /**< reason of blacklisting @todo define values */
  BL_description,     /**< description of the reason */
  BL_creationTime,    /**< creation timestamp */
  BL_validTo,         /**< expire timestamp */
  /** last entry to automatically count the number of BL_ entries */
  BLACKLIST_VARIABLE_FIELDCOUNT,
};

/**
 */
struct _BlacklistEB
{
  long  id;                  /**< Id of the blacklist item */
  long  msisdnId;            /**< the blacklisted MSISDN */
  long  serviceNumberId;     /**< the blacklisted service number */
  long  applicationId;       /**< the blacklisted application */
  long  keywordId;           /**< the blacklisted keyword (not supported now) */
  BlacklistType listingType; /**< hard/soft blacklisting */
  BlacklistReason reason;    /**< reason of blacklisting @todo define values */
  Octstr*  description;      /**< description of the reason */
  TimeVal* creationTime;     /**< creation timestamp */
  long  creationDate;        /**< simple creation date */
  long  valid_to;            /**< expiration timestamp */

  int   dirty;               /**< dirty flags */
};

/**
 * Dump the blacklist object to the log.
 *
 * @param self object
 * @param contextInfo which is printed after the id and before the details.
 */
static void blacklistEB_dump(const BlacklistEB* self, const char* contextInfo);


/**
 * Define the bitmask for each blacklist field.
 *
 * @note The BLACKLIST_VARIABLE_FIELDCOUNT must be used only to determine
 *       the size of the array - do not include it in the array.
 */
long blacklistEB_field_id[BLACKLIST_VARIABLE_FIELDCOUNT] =
{
  1 << (int)BL_id,              /**< Id of the blacklist item */
  1 << (int)BL_applicationId,   /**< the blacklisted application */
  1 << (int)BL_keywordId,       /**< the blacklisted keyword (not supported now) */
  1 << (int)BL_serviceNumberId, /**< the blacklisted service number */
  1 << (int)BL_msisdnId,        /**< the blacklisted MSISDN */
  1 << (int)BL_listingType,     /**< hard/soft blacklisting */
  1 << (int)BL_reason,          /**< reason of blacklisting @todo define values */
  1 << (int)BL_description,     /**< description of the reason */
  1 << (int)BL_creationTime,    /**< creation timestamp */
  1 << (int)BL_validTo,         /**< blacklist expiration */
};

bool blacklistEB_blacklistingEnabled()
{
  configuration_manager* cfg = psms_get_configuration_manager(psms_get_service());
  return !cfg->instance_config->core.disable_blacklist;
}

/**
 * create a new blacklist entry.
 * @note msisdnId and serviceNumberId cannot be changed later.
 */
BlacklistEB* blacklistEB_create(long msisdnId,
                                long serviceNumberId,
                                long applicationId,
                                BlacklistType   listingType,
                                BlacklistReason reasonCode,
                                Octstr* desc,
                                long valid_to)
{
  if (!blacklistEB_blacklistingEnabled())
  {
    dgw_error(DGW_DO_YOU_KNOW_WHAT_YOURE_DOING,
              "blacklistEB_find: called, but no blacklist handling on this instance! "
              "Use blacklistManager function instead of blacklistEB!");
    return NULL;
  }

  if (msisdnId <= 0)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE, "Blacklist: msisdnId %ld invalid", msisdnId);
    return NULL;
  }

  if (serviceNumberId < 0)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE,
              "Blacklist[msisdnId:%ld]: serviceNumberId %ld invalid",
              msisdnId, serviceNumberId);
    return NULL;
  }

  if (applicationId < 0)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE,
              "Blacklist[msisdnId:%ld]: applicationId %ld invalid",
              msisdnId, applicationId);
    return NULL;
  }

  if (listingType < BLACKLIST_SOFT || listingType >= BLACKLIST_TYPE_COUNT)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE,
              "Blacklist[msisdnId:%ld]: listingType %d invalid",
              msisdnId, listingType);
    return NULL;
  }

  if (reasonCode <= BL_REASON_UNDEFINED || reasonCode >= BLACKLIST_REASON_COUNT)
  {
    dgw_error(DGW_INVALID_PARAMETER_VALUE,
              "Blacklist[msisdnId:%ld]: reasonCode %d invalid",
              msisdnId, reasonCode);
    return NULL;
  }

  if (octstr_len(desc) <= 0)
  {
    dgw_warn("Blacklist[msisdnId:%ld]: empty description "
             "for serviceNumberId:%ld",
             msisdnId, serviceNumberId);
    return NULL;
  }

  BlacklistEB* self = gw_malloc(sizeof(BlacklistEB));
  self->id = 0;

  self->msisdnId        = msisdnId;
  self->serviceNumberId = serviceNumberId;
  self->applicationId   = applicationId;
  self->keywordId       = 0L;
  self->listingType     = listingType;
  self->reason          = reasonCode;
  self->description     = desc;
  self->creationTime    = get_timeUsec((TimeVal*)NULL);
  self->creationDate    = self->creationTime->tv_sec;
  self->valid_to        = valid_to;
  self->dirty           = 0;

  return self;
}

/**
 * query to get the blacklist entries
 * @note Keep in sync with method blacklistEB_reconstruct
 */
#define FMT_FINDBLACKLIST "SELECT id, msisdn_id, service_number_id, application_id" \
  ", keyword_id, listing_type, reason, description" \
  ", to_char(creation_time,'YYYY-MM-DD HH24:MI:SS.FF'), valid_to, creation_date " \
  "FROM blacklist " \
  "WHERE msisdn_id = :1 "

/**
 * Optional second condition on service_number_id
 */
#define FMT_FINDBLACKLIST_WHERESVC "AND (service_number_id IS NULL OR service_number_id IN (0, :2)) "

/**
 * Optional third condition on application
 */
#define FMT_FINDBLACKLIST_WHEREAPP "AND (application_id IS NULL OR application_id IN (0, :3)) "
/**
 * Optional condition on listing type
 */
#define FMT_FINDBLACKLIST_WHERETYPE "AND (listing_type = :2) "

/**
 * Fill row of a database query into a BlacklistEB object.
 *
 * If there is no existing BlacklistEB passed, a new is created.
 *
 * @param oldOjb existing Object or NULL for a completely new one
 * @param fields Fields retreived from the database in query order.
 * @return new or updated object.
 * @retval NULL if the record was empty or incorrect.
 */
static BlacklistEB* blacklistEB_reconstruct(List* fields)
{
  BlacklistEB* self = gw_malloc(sizeof(BlacklistEB));

  if (gwlist_len(fields) < 11)
  {
    dgw_error(DGW_QUERY_FAILED,
              "ORACLE: Error while selecting Blacklist - got %ld fields",
              gwlist_len(fields));
    return self;
  }

  self->id = octstrList_extractLong(fields);                // 1
  self->msisdnId = octstrList_extractLong(fields);          // 2
  self->serviceNumberId = octstrList_extractLong(fields);   // 3
  self->applicationId = octstrList_extractLong(fields);     // 4
  self->keywordId = octstrList_extractLong(fields);         // 5
  self->listingType = (BlacklistType)octstrList_extractInt(fields); // 6
  self->reason = (BlacklistReason)octstrList_extractInt(fields); // 7
  self->description = octstrList_extractOctstr(fields);     // 8
  self->creationTime = octstrList_extractTimeVal(fields);   // 9
  self->valid_to = octstrList_extractLong(fields);          //10
  self->creationDate = octstrList_extractLong(fields);      //11

  if (gwlist_len(fields) > 0)
  {
    dgw_warn("Blacklist[%ld] %ld fields not processed.", self->id, gwlist_len(fields));
  }

  blacklistEB_dump(self, "reconstruct");
  return self;
}

#define BLACKLISTEB_IRRELEVANT_ID 0
/**
 * Searches for a blacklist entry with parameters.
 *
 * @param msisdnId mandatory
 * @param serviceNumberId mandatory for soft blacklisting, optional for hard blacklisting
 * @param applicationId optional
 * @param listingType BLACKLIST_SOFT searches for soft and hard blacklisting
 *                    BLACKLIST_HARD searches only for hard blacklists.
 */
LinkedList* blacklistEB_find(persistence_manager* pmgr,
                             long msisdnId,
                             long serviceNumberId,
                             long applicationId
                            )
{
  /*
   APPLICATION_ID      NUMBER(20)
   KEYWORD_ID          NUMBER(20)
   SERVICE_NUMBER_ID   NUMBER(20)
   MSISDN_ID           NUMBER(20)
   LISTING_TYPE        NUMBER(2)
   REASON              NUMBER(5)
   CREATION_DATE       NUMBER(20)
   ID                  NOT NULL NUMBER(20)
   DESCRIPTION         VARCHAR2(160)
   CREATION_TIME       TIMESTAMP(6) WITH LOCAL TIME ZONE
   VALID_TO            NUMBER(10)
  */
  Octstr* os_serviceNumberId = NULL;
  Octstr* os_applicationId   = NULL;
  Octstr* sql                = NULL;

  if (!blacklistEB_blacklistingEnabled())
  {
    dgw_error(DGW_DO_YOU_KNOW_WHAT_YOURE_DOING,
              "blacklistEB_find: called, but no blacklist handling on this instance! "
              "Use blacklistManager function instead of blacklistEB!");
    return NULL;
  }

  static Octstr* sql_findBlacklist_whereSvcApp = NULL;
  if (sql_findBlacklist_whereSvcApp == NULL)
  {
    sql_findBlacklist_whereSvcApp = octstr_imm(FMT_FINDBLACKLIST
                                    FMT_FINDBLACKLIST_WHERESVC FMT_FINDBLACKLIST_WHEREAPP);
    dgw_debug("initiated: %s", octstr_get_cstr(sql_findBlacklist_whereSvcApp));
  }

  static Octstr* sql_findBlacklist_whereSvc = NULL;
  if (sql_findBlacklist_whereSvc == NULL)
  {
    sql_findBlacklist_whereSvc = octstr_imm(FMT_FINDBLACKLIST FMT_FINDBLACKLIST_WHERESVC);
    dgw_debug("initiated: %s", octstr_get_cstr(sql_findBlacklist_whereSvc));
  }

  static Octstr* sql_findBlacklist = NULL;
  if (sql_findBlacklist == NULL)
  {
    sql_findBlacklist = octstr_imm(FMT_FINDBLACKLIST);
    dgw_debug("initiated: %s", octstr_get_cstr(sql_findBlacklist));
  }

  List* binds = gwlist_create();
  Octstr* os_msisdnId = octstr_format("%ld", msisdnId);
  gwlist_append(binds, os_msisdnId);        /* :1 */

  if (serviceNumberId != BLACKLISTEB_IRRELEVANT_ID)
  {
    os_serviceNumberId = octstr_format("%ld", serviceNumberId);
    gwlist_append(binds, os_serviceNumberId); /* :2 */

    if (applicationId != BLACKLISTEB_IRRELEVANT_ID)
    {
      os_applicationId   = octstr_format("%ld", applicationId);
      gwlist_append(binds, os_applicationId);   /* :3 */
      sql = sql_findBlacklist_whereSvcApp;
    }
    else
    {
      /* application id is zero */
      sql = sql_findBlacklist_whereSvc;
    }
  }
  else
  {
    /* service number id is zero */
    sql = sql_findBlacklist;
  }

  LinkedList* result = dbOracle_selectObjects(pmgr, sql, binds,
                       RECONSTRUCT_OBJECT_CALLBACK(blacklistEB_reconstruct));
  gwlist_destroy(binds, octstr_destroy_item);

  return result;
}

/**
 * Persist the blacklist entry in the database
 */
int blacklistEB_persist(BlacklistEB* self, persistence_manager* persistenceMgr)
{
  int rc = -1;
  static Octstr* sql_insertBlacklist = NULL;

  if (!blacklistEB_blacklistingEnabled())
  {
    dgw_error(DGW_DO_YOU_KNOW_WHAT_YOURE_DOING,
              "blacklistEB_persist: called, but no blacklist handling on this instance! "
              "Use blacklistManager function instead of blacklistEB!");
    return rc;
  }

  if (NULL == self)
  {
    dgw_error(DGW_DO_YOU_KNOW_WHAT_YOURE_DOING, "blacklistEB_persist: called with this == NULL!");
    return rc;
  }

  if (sql_insertBlacklist == NULL)
  {
    // check is removed, recommended by PSC
    sql_insertBlacklist = octstr_imm("INSERT INTO blacklist "
                                     "( id, msisdn_id, service_number_id, application_id"
                                     ", keyword_id, listing_type, reason, description, valid_to, creation_date) "
                                     "VALUES (:1, :2, :3, :4, :5, :6, :7, :8, :9, :10)");
  }

  // get id for blacklist entry
  if (self->id <= 0)
  {
    long new_id = psms_get_next_blacklist_id(persistenceMgr);
    if (new_id <= 0)
    {
      return rc;
    }
    self->id = new_id;
  }

  // store in DB
  List* binds = gwlist_create();
#define ADD_LONG(val) { gwlist_append(binds, octstr_format("%ld", val)); }
#define ADD_INT(val)  { gwlist_append(binds, octstr_format("%d", val));  }
#define ADD_STR(val)  { gwlist_append(binds, octstr_format("%s", val));  }
#define ADD_NULL      ADD_STR("")
  /* use the Octstr as it is */
#define ADD_OCTSTR(val)  { gwlist_append(binds, octstr_duplicate(val));  }

  ADD_LONG(self->id);                // 1
  ADD_LONG(self->msisdnId);          // 2

  if (self->serviceNumberId != 0)
  {
    ADD_LONG(self->serviceNumberId);   // 3
  }
  else
  {
    ADD_NULL;
  }
  if (self->applicationId != 0)
  {
    ADD_LONG(self->applicationId);     // 4
  }
  else
  {
    ADD_NULL;
  }
  if (self->keywordId != 0)
  {
    ADD_LONG(self->keywordId);         // 5
  }
  else
  {
    ADD_NULL;
  }
  ADD_INT(self->listingType);        // 6
  ADD_INT(self->reason);             // 7
  ADD_OCTSTR(self->description);     // 8
  ADD_LONG(self->valid_to);          // 9
  ADD_LONG(self->creationDate);      // 10
  //#undef ADD_LONG
  //#undef ADD_INT
#undef ADD_OCTSTR
#undef ADD_STR
#undef ADD_NULL
  rc = dbOracle_update(persistenceMgr, sql_insertBlacklist, binds, NULL, NULL);
  gwlist_destroy(binds, octstr_destroy_item);
  dgw_debug("Blacklist[%ld:%p] inserted=%d: [msisdnId:%ld] [svcNumberId:%ld] "
            "[app-Id:%ld]",
            self->id, self, rc,
            self->msisdnId,
            self->serviceNumberId,
            self->applicationId
           );

  return rc;
}

static const char* fmt_deleteBlacklist_id = "DELETE from blacklist WHERE id = :1";

/**
 * Delete the blacklist entry from the database.
 */
int blacklistEB_delete(const BlacklistEB* self, persistence_manager* persistenceMgr)
{
  if (!blacklistEB_blacklistingEnabled())
  {
    dgw_error(DGW_DO_YOU_KNOW_WHAT_YOURE_DOING,
              "blacklistEB_delete: called, but no blacklist handling on this instance! "
              "Use blacklistManager function instead of blacklistEB!");
    return -1;
  }

  dgw_assert(self);

  if (self->id == 0)
  {
    dgw_trace("blacklist entry with id[%ld] is not deleted", self->id);
    return 0;
  }

  /** do not delete hard blacklisted items */
  if (self->listingType == BLACKLIST_HARD &&
      blacklistEB_isValidBlacklist(self))
  {
    dgw_warn("cannot delete valid HARD blacklist entry[%ld] on "
             "[msisdnId:%ld] [svcNumberId:%ld] [app-Id:%ld]",
             self->id,
             self->msisdnId,
             self->serviceNumberId,
             self->applicationId
            );
    return 0;
  }

  static Octstr* sql_deleteBlacklist = NULL;
  if (sql_deleteBlacklist == NULL)
  {
    sql_deleteBlacklist = octstr_imm(fmt_deleteBlacklist_id);
  }

  List* binds = gwlist_create();
  gwlist_append(binds, octstr_format("%ld", self->id));        /* :1 */
  int rc = dbOracle_update(persistenceMgr, sql_deleteBlacklist, binds, NULL, NULL);
  if (rc >= 1)
  {
    dgw_info("blacklist entry deleted, id[%ld] msisdnId[%ld] svcNumberId[%ld] appId[%ld]",
             self->id,
             self->msisdnId,
             self->serviceNumberId,
             self->applicationId);
  }
  else if (rc == 0)
  {
    dgw_debug("no blacklist entry[%ld] was deleted", self->id);
  }
  else
  {
    dgw_warn("error %d during deletion of blacklist entry[%ld]", rc, self->id);
  }
  gwlist_destroy(binds, octstr_destroy_item);

  return rc;
}

/**
 * Destroy the blacklist entry in memory.
 */
void blacklistEB_destroy(BlacklistEB* self)
{
  FREE_AND_NULL(self->description, octstr_destroy);
  FREE_AND_NULL(self->creationTime);
  gw_free(self);
}


/* getter methotds --------------------------------------------*/
long blacklistEB_getId(const BlacklistEB* self)
{
  return (self) ? self->id : -1;
}

Octstr* blacklistEB_getDescription(const BlacklistEB* self)
{
  return (self) ? self->description : NULL;
}

long blacklistEB_getApplicationId(const BlacklistEB* self)
{
  return (self) ? self->applicationId : -1;
}

long blacklistEB_getKeywordId(const BlacklistEB* self)
{
  return (self) ? self->keywordId : -1;
}

long blacklistEB_getServiceNumberId(const BlacklistEB* self)
{
  return (self) ? self->serviceNumberId : -1;
}

long blacklistEB_getMsisdnId(const BlacklistEB* self)
{
  return (self) ? self->msisdnId : -1;
}

BlacklistType blacklistEB_getListingType(const BlacklistEB* self)
{
  return (self) ? self->listingType : BLACKLIST_UNKNOWN;
}

BlacklistReason blacklistEB_getReason(const BlacklistEB* self)
{
  return (self) ? self->reason : BL_REASON_UNDEFINED;
}

long blacklistEB_getCreationDate(const BlacklistEB* self)
{
  return (self) ? self->creationDate : -1;
}

TimeVal* blacklistEB_getCreationTime(const BlacklistEB* self)
{
  return (self) ? self->creationTime : NULL;   // NULL not implemented yet
}

long blacklistEB_getValidTo(const BlacklistEB* self)
{
  return (self) ? self->valid_to : -1;
}

bool blacklistEB_isValid(long valid_to)
{
  return (valid_to == 0 || valid_to > time(NULL));
}

bool blacklistEB_isValidBlacklist(const BlacklistEB* self)
{
  dgw_assert(NULL != self);
  return (blacklistEB_isPermanentBlacklist(self) || self->valid_to > time(NULL));
}

bool blacklistEB_isPermanentBlacklist(const BlacklistEB* self)
{
  dgw_assert(NULL != self);
  return (self->valid_to == 0);
}

void blacklistEB_setCreationDate(BlacklistEB* self, long creationDate)
{
  self->creationDate = creationDate;
  self->creationTime->tv_sec = creationDate;
}
/**
 * Dump the blacklist to the log file.
 */
static void blacklistEB_dump(const BlacklistEB* self, const char* contextInfo)
{
  Octstr* creation = timeValToString(self->creationTime);

  dgw_debug("blacklist context[%s]", (contextInfo != NULL) ? contextInfo : PSMS_NOTAVAILABLE);
  dgw_debug("  [%ld]: msisdn_id[%ld] svcnum_id[%ld] appid[%ld] keywordid[%ld] listing_type[%d] "
            "reason[%d] description[%s] creationtime[%s] creationdate[%ld] valid_to[%ld]",
            self->id,
            self->msisdnId,
            self->serviceNumberId,
            self->applicationId,
            self->keywordId,
            self->listingType,
            self->reason,
            (self->description != NULL) ? octstr_get_cstr(self->description) : PSMS_NOTAVAILABLE,
            octstr_get_cstr(creation),
            self->creationDate,
            self->valid_to
           );

  octstr_destroy(creation);
}

/*
 * selects entries by msisdn id and listing type, IS THERE A DATABASE INDEX ON THESE COLUMNS
 *                    and svcnum id, appid, valid_to
 */
LinkedList* blacklistEB_select_on_listingtype(persistence_manager* pmgr, const long msisdn_id,
    const BlacklistType type,
    const long svcnum_id, const long app_id)
{
  Octstr* sql_findBlacklist_whereType = octstr_create(FMT_FINDBLACKLIST FMT_FINDBLACKLIST_WHERETYPE);

  List* binds = gwlist_create();
  Octstr* o_msisdn_id = octstr_format("%ld", msisdn_id);
  Octstr* o_type = octstr_format("%d", type);

  gwlist_append(binds, o_msisdn_id);
  gwlist_append(binds, o_type);

  unsigned short idx = 3;
  if (svcnum_id != 0)
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (service_number_id IS NULL or service_number_id IN (0, :%d)) ", idx++);
    gwlist_append(binds, octstr_format("%ld", svcnum_id));
  }
  if (app_id != 0)
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (application_id IS NULL or application_id IN (0, :%d)) ", idx++);
    gwlist_append(binds, octstr_format("%ld", app_id));
  }

  LinkedList* result = dbOracle_selectObjects(pmgr, sql_findBlacklist_whereType, binds,
                       RECONSTRUCT_OBJECT_CALLBACK(blacklistEB_reconstruct));
  gwlist_destroy(binds, octstr_destroy_item);
  octstr_destroy(sql_findBlacklist_whereType);
  return result;
}

LinkedList* blacklistEB_select_on_listingtype_validto(persistence_manager* pmgr, const long msisdn_id,
    const long svcnumid, const long appid,
    const BlacklistType type, const long valid_to)
{
  Octstr* sql_findBlacklist_whereType = octstr_create(FMT_FINDBLACKLIST FMT_FINDBLACKLIST_WHERETYPE);

  List* binds = gwlist_create();
  Octstr* o_msisdn_id = octstr_format("%ld", msisdn_id);
  Octstr* o_type = octstr_format("%ld", type);

  gwlist_append(binds, o_msisdn_id);
  gwlist_append(binds, o_type);
  int idx = 3;
  if (svcnumid == 0)
  {
    octstr_append_cstr(sql_findBlacklist_whereType, "AND (service_number_id IS NULL OR service_number_id = 0) ");
  }
  else
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (service_number_id = :%d) ", idx++);
    gwlist_append(binds, octstr_format("%ld", svcnumid));
  }

  if (appid == 0)
  {
    octstr_append_cstr(sql_findBlacklist_whereType, "AND (application_id IS NULL OR application_id = 0) ");
  }
  else
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (application_id = :%d) ", idx++);
    gwlist_append(binds, octstr_format("%ld", appid));
  }

  octstr_append_cstr(sql_findBlacklist_whereType, "AND (valid_to is NULL OR valid_to = 0) ");

  if (valid_to == 0)
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (valid_to > :%d) ", idx++);
    gwlist_append(binds, octstr_format("%ld", get_timestamp()));
  }
  else
  {
    octstr_format_append(sql_findBlacklist_whereType, "AND (valid_to >= :%d) ", idx++);
    gwlist_append(binds, octstr_format("%ld", valid_to));
  }

  LinkedList* result = dbOracle_selectObjects(pmgr, sql_findBlacklist_whereType, binds,
                       RECONSTRUCT_OBJECT_CALLBACK(blacklistEB_reconstruct));
  gwlist_destroy(binds, octstr_destroy_item);
  FREE_AND_NULL(sql_findBlacklist_whereType, octstr_destroy);

  return result;
}

bool
blacklist_exist_entry(persistence_manager* pmgr,
                      const long msisdn_id,
                      const long svcnumid,
                      const long appid,
                      BlacklistType type,
                      const long valid_to)
{

  dgw_debug("%s uses listing type[%d] and valid to[%ld]", __func__, type, valid_to);
  LinkedList* result = blacklistEB_select_on_listingtype_validto(pmgr, msisdn_id, svcnumid, appid, type, valid_to);
  bool rc = (linkedList_itemCount(result) > 0);
  FREE_AND_NULL(result, linkedList_destroyCustom, DESTROY_CALLBACK(blacklistEB_destroy));
  return rc;
}

bool blacklistEB_isExpired(const BlacklistEB* entry, long period)
{
  time_t now = time(NULL);
  bool expired = ((period != 0) ? (entry->creationDate + period) < now : false) ||
                 !blacklistEB_isValid(entry->valid_to);

  return expired;
}
/* ----------------------------------------------------------------- */
/** @} */
