import { createUserModel } from "/src/type/user.js";

export function createAutoEnterVO(data = {}){
    return createUserModel(data)
}

