#ifndef TRANSACT_BLACKLIST_BLACKLISTEB_H
#define TRANSACT_BLACKLIST_BLACKLISTEB_H 1

/**
 * @addtogroup blacklistEB
 * @ingroup blacklist
 * Blacklisting for MSISDN
 */
/** @{ */
/**
 * @file
 * Definitions for BlacklistEB
 *
 * @copydoc transact::blacklist::BlacklistEB
 * @copydoc Blacklist
 * @author DI Andreas Welzl (http://xion.at)
 * @version $Header$
 */
/* ----------------------------------------------------------------- */

#include "dgw/persistence.h"
#include "util/collection/linkedList.h"
#include "util/util.h"

/** Type of Blacklisting */
typedef enum
{
  BLACKLIST_UNKNOWN = -1,       /**< unknown type */
  BLACKLIST_SOFT = 0,           /**< SOFT blacklist is removed by next MO to the service-number */
  BLACKLIST_HARD = 1,           /**< HARD blacklist can be removed only by the administrator */
  BLACKLIST_TMP_REJ = 2,        /**< TMP_REJ for Suissecom: removed by successfully billed MT delivery, converted to soft after 4 weeks*/
  BLACKLIST_TMP_REJ_RETRY = 3,  /**< TMP_REJ_RETRY for Suissecom: created for every retry, cleared if older than 1 week, blocks further retries for one week*/
  BLACKLIST_MAX_AQM = 4,        /**< MAX_AQM for ONE_AQM... created if user exceeds max AQM count without a paid message sent/received */

  BLACKLIST_TYPE_COUNT,         /**< Counter @note must be the last entry on this enum */
} BlacklistType;

typedef enum
{
  BL_REASON_UNDEFINED           = -1, /**< No reason defined */
  BL_REASON_CUSTOM              =  0, /**< Custom reason - see description */
  BL_REASON_TERMINATION_KEYWORD =  1, /**< dynamic blacklisting by termination keyword */
  BL_REASON_WAITING_IN_AQM      =  2, /**< dynamic blacklisting by AQM state */
  BL_REASON_SMSC_REJECTED       =  3, /**< dynamic blacklisting by special SMSC reject */
  BL_REASON_SMS_SPERRE          =  4, /**< dynamic blacklisting via Internet (www.sms-sperre.at) */
  BL_REASON_TMP_REJ_TO_SOFT     =  5, /**< dynamic blacklisting: TMP_REJ older than 4 weeks is converted to SOFT */
  BL_REASON_MANUAL              =  6, /**< manual blacklist entry*/
  BL_REASON_SESSION_KILLER      =  7, /**< blacklisted by session killer tool */
  BL_REASON_LIMIT_REACHED       =  8, /**< dynamic blacklisting: limit for session reached - interrupt session*/
  BL_REASON_CUSTOMER_MANUAL     =  9, /**< manual blacklist by customer complain*/
  BL_REASON_OPERATOR_MANUAL     = 10, /**< manual blacklist by ticket complain*/
  BL_REASON_HU_REACTIVATE       = 11, /**< dynamic temporary blacklisting to prevent reactivation of service within 24h (CoE HU)*/
  BL_REASON_SMSC_REJECTED_TEMP  = 12, /**< dynamic temporary softblacklist after negative SMSC response*/
  BL_REASON_24H_FIXED_SOFT      = 13, /**< dynamic temporary 24H fixed (MO can NOT remove it) soft blacklist */
  BL_REASON_FROM_TOOL           = 14, /**< reason from daemon */

  BLACKLIST_REASON_COUNT, /**< Counter @note must be the last entry on this enum */
} BlacklistReason;

/*
id
description
applicationId
keywordId
serviceNumberId
msisdnId
listingType
reason
creationDate
creationTime

Name               Null?    Type
 ----------------- -------- ---------------------------------
 APPLICATION_ID             NUMBER(20)
 KEYWORD_ID                 NUMBER(20)
 SERVICE_NUMBER_ID          NUMBER(20)
 MSISDN_ID                  NUMBER(20)
 LISTING_TYPE               NUMBER(2)
 REASON                     NUMBER(5)
 CREATION_DATE              NUMBER(20)
 ID                NOT NULL NUMBER(20)
 DESCRIPTION                VARCHAR2(160)
 CREATION_TIME              TIMESTAMP(6) WITH LOCAL TIME ZONE
 */

/**
 */
typedef struct _BlacklistEB BlacklistEB;

/**
 * reload sys-disable-blacklist from instance_config for blacklistEB
 */

/**
 * check if we handle blacklists on this instance
 *
 * @return true if we handle blacklists
 * @return false if we don't handle blacklists
 **/
bool blacklistEB_blacklistingEnabled(void);

/**
 * create a new blacklist entry.
 * @note msisdnId and serviceNumberId cannot be changed later.
 *
 * @param msisdnId mandatory
 * @param serviceNumberId mandatory for soft blacklisting,
                         optional for hard blacklisting
 * @param applicationId optional
 * @param listingType type of blacklisting: soft or hard
 * @param reasonCode  Number to identify a standard reason
 *                    If BL_REASON_CUSTOM the description parameter should
                      contain the information.
 * @param description Describes the blacklist reason.
 *               Copy this value before calling if you want to use it later.
 * @param valid_to    timestamp of when the blacklist expire
 */
BlacklistEB* blacklistEB_create(long msisdnId,
                                long serviceNumberId,
                                long applicationId,
                                BlacklistType   listingType,
                                BlacklistReason reasonCode,
                                Octstr* description,
                                long valid_to
                               );

/**
 * Searches for a blacklist entry with parameters.
 *
 * @param persistenceMgr persistence manager who holds the connection pool
 * @param msisdnId mandatory
 * @param serviceNumberId mandatory for soft blacklisting,
                         optional for hard blacklisting
 * @param applicationId optional
 */
LinkedList* blacklistEB_find(persistence_manager* persistenceMgr,
                             long msisdnId,
                             long serviceNumberId,
                             long applicationId
                            );
/*
 * @param listingType BLACKLIST_SOFT searches for soft and hard blacklisting
 *                    BLACKLIST_HARD searches only for hard blacklists.
 */

/**
 * Persist the blacklist entry in the database
 *
 * @param this the blacklist entry
 * @param persistenceMgr persistence manager who holds the connection pool
 */
int blacklistEB_persist(BlacklistEB* self, persistence_manager* persistenceMgr);

/**
 * Delete the blacklist entry from the database.
 *
 * @note It does not delete hard blacklisted items (this is an administration
 *       responsibility).
 *
 * @param this the blacklist entry
 * @param persistenceMgr persistence manager who holds the connection pool
 * @return number of deleted records
 * @retval 1 success
 * @retval 0 deleted an entry which was not persisted or hard blacklisted
             - nothing to do
 * @retval -1 or other: an error occured
 */
int blacklistEB_delete(const BlacklistEB* self, persistence_manager* persistenceMgr);

/**
 * Destroy the blacklist entry in memory.
 */
void blacklistEB_destroy(BlacklistEB* self);

int blacklist_delete_entry(persistence_manager* pmgr, const long msisdnid,
                           const long svcnum_id,
                           const long app_id,   const BlacklistType type);

/* getter methods */
long blacklistEB_getId(const BlacklistEB* self);
Octstr* blacklistEB_getDescription(const BlacklistEB* self);
long blacklistEB_getApplicationId(const BlacklistEB* self);
long blacklistEB_getKeywordId(const BlacklistEB* self);
long blacklistEB_getServiceNumberId(const BlacklistEB* self);
long blacklistEB_getMsisdnId(const BlacklistEB* self);
BlacklistType blacklistEB_getListingType(const BlacklistEB* self);
BlacklistReason blacklistEB_getReason(const BlacklistEB* self);
long blacklistEB_getCreationDate(const BlacklistEB* self);
TimeVal* blacklistEB_getCreationTime(const BlacklistEB* self);
long blacklistEB_getValidTo(const BlacklistEB* self);
/**
 * check if timestamp for a blacklist is still valid
 *
 * @return true if it is a permanent blacklist or not expired yet,
 *         false if blacklist is expired
 */
bool blacklistEB_isValid(long valid_to);
/**
 * check if blacklist is still valid
 *
 * @param must not be NULL
 * @return true if it is a permanent blacklist or not expired yet,
 *         false if blacklist is expired
 */
bool blacklistEB_isValidBlacklist(const BlacklistEB* self);
/**
 * check if blacklist is permanent
 *
 * @param must not be NULL
 * @return true if it is a permanent blacklist
 *         false if blacklist is expired or will be expired
 */
bool blacklistEB_isPermanentBlacklist(const BlacklistEB* self);

/* setter methods */
void blacklistEB_setDescription(BlacklistEB* self, Octstr* desc);
void blacklistEB_setApplicationId(BlacklistEB* self, long id);
void blacklistEB_setKeywordId(BlacklistEB* self, long id);
void blacklistEB_setListingType(BlacklistEB* self, BlacklistType listingType);
void blacklistEB_setReason(BlacklistEB* self, BlacklistReason reasonCode);
void blacklistEB_setCreationDate(BlacklistEB* self, long creationDate);

LinkedList* blacklistEB_select_on_listingtype(persistence_manager* pmgr,
    const long msisdn_id,
    const BlacklistType type,
    const long svcnum_id,
    const long app_id);

LinkedList* blacklistEB_select_on_listingtype_validto(persistence_manager* pmgr,
    const long msisdn_id,
    const long svcnumid,
    const long appid,
    const BlacklistType type,
    const long valid_to);

bool blacklist_exist_entry(persistence_manager* pmgr,
                           const long msisdn_id,
                           const long svcnumid,
                           const long appid,
                           BlacklistType type,
                           const long valid_to);

bool blacklistEB_isExpired(const BlacklistEB* entry, long period);
/* ----------------------------------------------------------------- */
/** @} */

#endif // TRANSACT_BLACKLIST_BLACKLISTEB_H

