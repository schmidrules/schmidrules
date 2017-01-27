#ifndef TRANSACT_BILLING_BILLING_RECORD_H
#define TRANSACT_BILLING_BILLING_RECORD_H 1

#include "dgw/persistence.h"
#include "dgwlib/dgw_types.h"

enum BillingRecordFields
{
  BILLING_RECORD_MSISDN_ID,
  BILLING_RECORD_SVCNUM_ID,
  BILLING_RECORD_CTIME,
  BILLING_RECORD_STATUS,
  BILLING_RECORD_AMOUNT,
  BILLING_RECORD_MSG_ID,

  BILLING_RECORD_FIELD_COUNT
};

BillingRecord* billingRecord_init(long msisdn_id,
                                  long svcnum_id,
                                  int status,
                                  int amount,
                                  long msg_id,
                                  BillingUtil* parent);
void billingRecord_destroy(BillingRecord* self);
record_descriptor* billingRecord_create_record_descriptor(void);
void billingRecord_translate(void** rec, int field_id, char* raw, FieldType* typ, TranslationStep action);
void billingRecord_update_status(BillingRecord* self, int status);
void billingRecord_update_amount(BillingRecord* self, int amount);

#endif // TRANSACT_BILLING_BILLING_RECORD_H

