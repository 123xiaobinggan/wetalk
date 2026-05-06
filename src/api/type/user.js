export function createUserModel(data = {}) {
  return {
    userId: data.userId ?? null,
    accountId: data.accountId ?? null,
    username: data.username ?? '',
    avatar: data.avatar ?? '',
    sex: data.sex ?? '',
    personalSignature: data.personalSignature ?? '',
    areaName: data.areaName ?? '',
    areaCode: data.areaCode ?? '',
  }
}