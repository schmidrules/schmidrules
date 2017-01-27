#include "gwlib/gwlib.h"

#include "transact/billing/billing_record.h"
#include "dgw/psms.h"
#include "dgwlib/dgwcommon.h"
#include "transact/message/billingUtil.h"

struct _BillingRecord
{
  long msisdn_id;
  long svcnum_id;
  long ctime;
  int status;
  int amount;
  long msg_id;

  BillingUtil* parent;
  Dirtyflagset dirty;
};

BillingRecord* billingRecord_init(long msisdn_id,
                                  long svcnum_id,
                                  int status,
                                  int amount,
                                  long msg_id,
                                  BillingUtil* parent)
{
  BillingRecord* self = gw_malloc(sizeof(BillingRecord));
  self->msisdn_id = msisdn_id;
  self->svcnum_id = svcnum_id;
  self->ctime = get_timestamp();
  self->status = status;
  self->amount = amount;
  self->msg_id = msg_id;

  self->parent = parent;
  self->dirty = 0;
  return self;
}

void billingRecord_update_status(BillingRecord* self, int status)
{
  self->status = status;

  persistance_manager_set_dirty_flag(&self->dirty,
                                     billingUtil_get_record_descriptor(self->parent),
                                     BILLING_RECORD_STATUS);
}

void billingRecord_update_amount(BillingRecord* self, int amount)
{
  self->amount = amount;

  persistance_manager_set_dirty_flag(&self->dirty,
                                     billingUtil_get_record_descriptor(self->parent),
                                     BILLING_RECORD_AMOUNT);
}

static BillingRecord* billingRecord_create(void)
{
  return gw_malloc(sizeof(BillingRecord));
}

void billingRecord_destroy(BillingRecord* self)
{
  gw_free(self);
}

record_descriptor* billingRecord_create_record_descriptor(void)
{
  return persistance_manager_create_record_descriptor(
           RECORD_DESCRIPTOR_BILLING_RECORD,
           "billed_message",
           BILLING_RECORD_FIELD_COUNT,
           billingRecord_translate);
}

void billingRecord_translate(void** rec, int field_id, char* raw, FieldType* typ, TranslationStep action)
{
  BillingRecord* self = NULL;

  switch (action)
  {
    case PM_TRANSLATE_GET_SUBTYPE:
      break;

    case PM_TRANSLATE_PREINIT:
      *rec = billingRecord_create();
      break;

    case PM_TRANSLATE_RESET_DIRTYFLAGSET:
      self = *rec;
      persistance_manager_reset_dirtyflagset(&self->dirty);
      break;

    case PM_TRANSLATE_UNSET_DIRTYFLAG:
      self = *rec;
      record_descriptor* rd = billingUtil_get_record_descriptor(self->parent);
      persistance_manager_unset_dirty_flag(&self->dirty, rd, field_id);
      break;

    case PM_TRANSLATE_GET_DIRTYFLAGSET:
      self = *rec;
      memcpy(raw, &self->dirty, sizeof(self->dirty));
      break;

    case PM_TRANSLATE_TO_RAW:
    case PM_TRANSLATE_FROM_RAW:
      self = *rec;
    case PM_TRANSLATE_GET_TYPE:
    case PM_TRANSLATE_GET_COLUMNAME:
      switch (field_id)
      {
        case BILLING_RECORD_MSISDN_ID:
          persistance_manager_long_translate("msisdn_id", SAFE_MEMBER_PTR(self, msisdn_id), PM_NO_UPDATE, raw, typ,
                                             action);
          break;
        case BILLING_RECORD_SVCNUM_ID:
          persistance_manager_long_translate("svcno_id", SAFE_MEMBER_PTR(self, svcnum_id), PM_NO_UPDATE, raw, typ,
                                             action);
          break;
        case BILLING_RECORD_CTIME:
          persistance_manager_long_translate("ctime", SAFE_MEMBER_PTR(self, ctime), PM_NO_UPDATE, raw, typ,
                                             action);
          break;
        case BILLING_RECORD_STATUS:
          persistance_manager_int_translate("status", SAFE_MEMBER_PTR(self, status), PM_MAY_UPDATE, raw, typ,
                                            action);
          break;
        case BILLING_RECORD_AMOUNT:
          persistance_manager_int_translate("amount", SAFE_MEMBER_PTR(self, amount), PM_MAY_UPDATE, raw, typ,
                                            action);
          break;
        case BILLING_RECORD_MSG_ID:
          persistance_manager_long_translate("msg_id", SAFE_MEMBER_PTR(self, msg_id), PM_NO_UPDATE, raw, typ,
                                             action);
          break;
      };
      break;

    case PM_TRANSLATE_POSTINIT:
      break;

    case PM_TRANSLATE_RELEASE:
      FREE_AND_NULL(*rec, billingRecord_destroy);
      break;
  }
}
