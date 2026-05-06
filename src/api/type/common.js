// 通用返回码
export const ResultCode = {
  SUCCESS: 0,
  ERROR: 1,
  UNAUTHORIZED: 401,
}

// 判断是否成功
export function isSuccess(res) {
  return res && res.code === ResultCode.SUCCESS
}

// 通用返回结构
export function createApiResult(data = {}) {
  return {
    code: data.code ?? ResultCode.SUCCESS,
    msg: data.msg ?? '',
    data: data.data ?? null,
  }
}
