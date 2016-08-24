export class Message {
  static fromJSON (buffer) {
    return new Message(JSON.parse(buffer.toString()))
  }

  constructor ({ username, command, contents, time= '',userList=''}) {
    this.username = username
    this.command = command
    this.contents = contents
    //Formatted strings require additional information.
    //In order to provided this information without formatting strings server side additional fields are needed
    //For clients that do not support specified fields the program will default to using contents as the fully formatted expected string
    this.time=time
    this.userList=userList
  }

  toJSON () {
    return JSON.stringify({
      username: this.username,
      command: this.command,
      contents: this.contents,
      //additional fields for transfering and recieving data
      time: this.time,
      userList: this.userList
    })
  }

  toString () {
    return this.contents
  }

}
