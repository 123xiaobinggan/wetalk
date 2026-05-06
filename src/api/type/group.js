export function createGroupModel(data = {}) {
  return {
    groupId: data.groupId ?? null,
    sessionId: data.sessionId ?? null,
    groupName: data.groupName ?? '',
    groupAvatar: data.groupAvatar ?? '',
    ownerUserId: data.ownerUserId ?? null,
    announcement: data.announcement ?? '',
    createdTime: data.createdTime ?? '',
  }
}