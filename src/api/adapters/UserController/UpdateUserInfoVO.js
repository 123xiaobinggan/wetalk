import { createUserModel } from './user.js'

export function createUpdateUserInfoVO(data = {}) {
    return createUserModel(data)
}
