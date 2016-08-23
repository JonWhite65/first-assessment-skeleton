export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, time= '',userList=''}) {
    this.username = username
    this.command = command
    this.contents = contents
    //added an additional field
    this.time=time
    this.userList=userList
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      //added field
      time: this.time,
      userList: this.userList
    })
  }

  toString () {
    return this.contents
  }

}
