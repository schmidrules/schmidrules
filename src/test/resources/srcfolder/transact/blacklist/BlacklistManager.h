#ifndef TRANSACT_BLACKLIST_BLACKLISTMANAGER_H
#define TRANSACT_BLACKLIST_BLACKLISTMANAGER_H 1

/**
 * @addtogroup BlacklistManager
 * @ingroup blacklist
 * Blacklisting for MSISDN
 */
/** @{ */
/**
 * @file
 * Definitions for BlacklistManager
 *
 * @copydoc transact::blacklist::BlacklistManager
 * @copydoc Blacklist
 * @author DI Andreas Welzl (http://xion.at)
 * @version $Header$
 */
/* ----------------------------------------------------------------- */

#include "transact/blacklist/BlacklistEB.h"
#include "config/entity/Application.h"
#include "dgw/persistence.h"
#include "dgwlib/message.h"
#include "util/collection/linkedList.h"
#include "util/util.h"

#include <stdio.h>

/**
 * Searches for a blacklist entry with parameters for MO messages.
 *
 * If there are only soft blacklistings, they are removed.
 *
 * @param persistenceMgr persistence manager to use
 * @param msg message to check
 * @retval negative hard blacklist found (-2 is a special state for active tmp hardblacklist HU)
 * @retval 0 no blacklisting found
 * @retval positive number of soft blacklists found and removed.
 */
bool blacklistManager_is_blacklisted_on_mo(persistence_manager* persistenceMgr, message* msg);


/**
 * Searches for a blacklist entry with parameters for MT messages.
 *
 * It only returns the result - does not delete blacklists.
 *
 * @param persistenceMgr persistence manager to use
 * @param msg message to check
 * @param applicationType Types PSMS_APP_TYPE_ROUTING_WEB_INITIATE and
 *            PSMS_APP_TYPE_ROUTING_NO_BLACKLIST ignore the dynamic blacklisting
 *            by termination keyword (application must handle it)
 * @retval -1 hard blacklist found
 * @retval <-1 timestamp of max. valid (temporary hard blacklist)
 * @retval 0 no blacklisting found
 * @retval +1 soft blacklist found (permanent)
 * @retval >1 timestamp of max. valid to (temporary soft blacklist)
 * document psms_code and valid_end
 */
bool blacklistManager_is_blacklisted_on_mt(persistence_manager* persistenceMgr,
    message* msg,
    PsmsReturnCode* psms_code,
    long* valid_end);


/**
 * blacklists msisdn for optional svc_num and optional app_id
 *
 * @param pmgr corresponding persistence manager
 * @param msisdn_num msisdn to blacklist
 * @param msisdn_id misdn_id to blacklist, if not set then it is fetched from database by msisdn_num
 * @param svcnum_id the service number to restrict the blacklisting to (may be NULL)
 * @param app_id the application to restrict the blacklisting to (may be 0)
 * @param type type of blacklisting (must be one of enum BlacklistType)
 * @param reason reason for blacklisting (must be one of BlacklistReason)
 * @param description optional description text
 * @param valid_to expiration timestamp of blacklist entry
 *
 */
void blacklistManager_blacklist_msisdn(persistence_manager* pmgr,
                                       char* msisdn_num,
                                       long msisdn_id,
                                       long svcnum_id,
                                       long  app_id,
                                       BlacklistType type,
                                       BlacklistReason reason,
                                       const char* description,
                                       long valid_to);

/**
 * revokes blacklisting of msisdn for optional svc_num and optional app_id
 *
 * @param cfg corresponding persistence manager
 * @param msisdn_id msisdn to revoke blacklisting for
 * @param svcnum_id the service number to restrict the revoking to (may be NULL)
 * @param app_id the application to restrict the revoking to (may be 0)
 * @param type type of blacklisting (must be one of enum BlacklistType)
 *
 * @retval negative if an error occurs
 * @retval positive revoking succeeded
 */
int blacklistManager_revoke_blacklisting(persistence_manager* pmgr,
    long msisdn_id,
    long svcnum_id,
    long app_id,
    BlacklistType type);

/**
 * Check blacklisting for a given msisdn and optional service number and
 * an optional application ID. If an expiration time > 0 is passed, all
 * matching blacklist entries older than the given timespan are removed
 * from the datasource.
 *
 * @param pmgr is the corresponding persistence manager
 * @param msisdn_num msisdn to check
 * @param msisdn_id is the msisdn id to blacklist,
 * @param svcnum_id the service number to restrict the check
 * @param app_id the application to restrict the check
 * @param type type of blacklisting
 * @param expiration_timespan is the timespan given in seconds for expiring
 *        blacklist entries. If 0 is passed here no entries are expired.
 * @param creation_date is set to the creationDate value of the blacklisting entry
 *        in case the msisdn is blacklisted
 * @retval true, misdn is blacklisted
 * @retval false, msisdn is no blacklisted
 */
bool blacklistManager_is_msisdn_blacklisted(persistence_manager* pmgr,
    const char* msisdn,
    const long msisdn_id,
    long svcnum_id,
    long app_id,
    BlacklistType type,
    long expiration_timespan,
    time_t* creation_date);
/* ----------------------------------------------------------------- */
/** @} */

#endif // TRANSACT_BLACKLIST_BLACKLISTMANAGER_H

