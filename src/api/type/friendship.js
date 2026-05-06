export function createFriendshipModel(data = {}){
  return {
    friendUserId: data.friendId ?? null,
    remark: data.remark ?? '',
    blocked: data.blocked ?? false,
    hideMoments: data.hideMoments ?? false,
    hideFriendMoments: data.hideFriendMoments ?? false,
    createdTime: data.createdTime ?? '',
  }
}