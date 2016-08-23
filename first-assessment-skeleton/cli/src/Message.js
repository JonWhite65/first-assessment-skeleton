export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents,type, time,userList }) {
    this.username = username
    this.command = command
    this.contents = contents
    //added andditional field
    this.type= type
    this.time=time
    this.userList
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      //added field
      type: this.type,
      time: this.time,
      userList: this.userList
    })
  }

  toString () {
    return this.contents
  }

}
