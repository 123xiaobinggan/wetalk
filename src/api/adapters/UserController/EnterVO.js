import { createUserModel } from '/src/type/user.js'

export function createEnterVO(data={}){
    return {
        token: data.token,
        user: createUserModel(data.user)
    }
}