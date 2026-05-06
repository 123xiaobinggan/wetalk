function buildWsMessage({ type, event, clientMsgId, data }) {
  return {
    type,
    event,
    clientMsgId,
    data,
  }
}

module.exports = { buildWsMessage }
