import vorpal from 'vorpal'
import { words } from 'lodash'
import { connect } from 'net'
import { Message } from './Message'
var chalk = require('chalk')

export const cli = vorpal()

let username
let server
let ip
let previousCommand = ''

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  .mode('connect <username>, <ip>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    username = args.username
    this.log(username)
    ip = args.ip
    this.log('')
    this.log(ip)
    server = connect({ host: ip, port: 8080 }, () => {
      server.write(new Message({ username, command: 'connect' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let timeStamp=''
      let username=''
      let contents=''
      const recievedMessage =Message.fromJSON(buffer)
      if(recievedMessage.time===undefined){
        this.log(recievedMessage.contents)
      //    [ recievedMessage.time, ...contents]=words(recievedMessage.contents,/[^ ]+/g)
      //    contents= contents.join(' ')
      // timeStamp=recievedMessage.time
      // username=contents
      }
     else{
      timeStamp=recievedMessage.time
      username=recievedMessage.username

    timeStamp=chalk.red(timeStamp)
    username=chalk.magenta(username)
      if(recievedMessage.command!==undefined){
        if(recievedMessage.command === 'echo'){
          contents=chalk.blue(recievedMessage.toString())
          this.log(`${timeStamp} <${username}> (echo): ${contents}`)
          }
        else if(recievedMessage.command === 'broadcast'){
          contents=chalk.yellow(recievedMessage.toString())
          this.log(`${timeStamp} <${username}> (all): ${contents}`)
          }
        else if(recievedMessage.command === 'users'){
          this.log(`${timeStamp}: currently connected users:`)
          this.log(recievedMessage.userList)
            }

        else if(recievedMessage.command.charAt(0) === '@'){
          contents=chalk.white(recievedMessage.toString())
          this.log(`${timeStamp} <${username}> (whisper): ${contents}`)
          }
         else if (recievedMessage.command === 'connect'||recievedMessage.command === 'disconnect'){

          contents=chalk.red(recievedMessage.toString())
          this.log(`${timeStamp} <${username}> ${contents}`)
          }
          else{
            this.log('An error has occured')
            this.log(`${timeStamp} <${username}> ${contents}`)

          }
        }
      }
    })

    server.on('end', () => {
      cli.exec('exit')
    })
  })
  .action(function (input, callback) {
    let [ command, ...rest ] = words(input,/[^ ]+/g)
    let contents = rest.join(' ')
    //boolean for previous command implementation
    let hasCommand
    //loop for previous command
    do{
      hasCommand = false
    //sorts commands given by user gave hot keys to commands
      if (command === 'disconnect'||command==='1') {
        command='disconnect'
        server.end(new Message({ username, command }).toJSON() + '\n')
        previousCommand=command
      } else if (command === 'echo'||command==='2') {
        command= 'echo'
          server.write(new Message({ username, command, contents }).toJSON() + '\n')
          previousCommand=command
        }
    // @user addition boolean variable to indicate private
        else if(command.charAt(0) === '@'){
          server.write(new Message({ username, command, contents }).toJSON() + '\n')
          previousCommand=command
        }
    //brodcast command boolean variable to indicate not private
        else if(command==='broadcast'||command==='3'){
          command='broadcast'
          server.write(new Message({ username, command, contents }).toJSON() + '\n')
          previousCommand=command
        }
    //asks for user data
        else if(command === 'users'||command=='4'){
          command= 'users'
          server.write(new Message({ username, command, contents }).toJSON() + '\n')
          previousCommand=command
        }

        else {
          if(previousCommand===''){

            this.log(chalk.green(`Command <${command}> was not recognized`))
          }
          else{
            hasCommand= true
            contents=command+' '+contents
            command= previousCommand

          }
        }
    } while(hasCommand)

    callback()
  })
