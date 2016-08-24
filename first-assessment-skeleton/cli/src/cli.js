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
let joiner

cli
  .delimiter(cli.chalk['yellow']('ftd~$'))

cli
  //added <ip> input to allow client to suppy an ip address for the server they wish to connect to
  .mode('connect <username>, <ip>')
  .delimiter(cli.chalk['green']('connected>'))
  .init(function (args, callback) {
    //establish input parameters
    username = args.username
    ip = args.ip
    server = connect({ host: ip, port: 8080 }, () => {
      //giving an intial value to contents gives me a way to distiguish server side a regular message class vs a modified one.
      server.write(new Message({ username, command: 'connect',contents: 'Allow Special Connection' }).toJSON() + '\n')
      callback()
    })

    server.on('data', (buffer) => {
      let timeStamp=''
      let username=''
      let contents=''

      const recievedMessage =Message.fromJSON(buffer)
      if(recievedMessage.time===undefined&&recievedMessage.command!==''&&recievedMessage.command!==null){
        //if the time field is not defined we must assume the correct string is located in contents
        //!=='' keeps the
        if(recievedMessage.command === 'echo'){
        this.log(chalk.blue(recievedMessage.contents))
      }
        else if(recievedMessage.command === 'broadcast'){
            this.log(chalk.yellow(recievedMessage.contents))
      }
        else if(recievedMessage.command === 'users'){
            this.log(recievedMessage.contents)
        }
        else if(recievedMessage.command.charAt(0) === '@'){
            this.log(chalk.white(recievedMessage.contents))
        }
        else if (recievedMessage.command === 'connect'||recievedMessage.command === 'disconnect'){
          this.log(chalk.red(recievedMessage.contents))
        }
        else{
          this.log(chalk.cyan(recievedMessage.contents))
        }
      }
     else if(recievedMessage.command!==''&&recievedMessage.command!==null){
       //using the additional fields the client can customize how they wish thier content to display
       //uses colors to cause diffrent segements of a responce to be diffrent colors.
      timeStamp=recievedMessage.time
      username=recievedMessage.username

    timeStamp=chalk.grey(timeStamp)
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
            contents=chalk.cyan(recievedMessage.toString())
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
    //regExp in the words function forces the words function to match any charater except a space.
    //this allows the @ of @user to be accepted
    let [ command, ...rest ] = words(input,/[^ ]+/g)
    let contents = rest.join(' ')
    //boolean for previous command implementation
    let hasCommand
    //loop for previous command loop is initialy set to false and will be ignored
    //only activated if command is invalid and previous command was not
    do{
      hasCommand = false
    //sorts commands given by user as well as implemented hot keys for commands
      if (command === 'disconnect'||command==='1') {
        //ensures hot keys do not get sent to server
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
    //allows one server to join into another server
        else if(command==='join'||command==='5'){
          command= 'join'
          joiner=contents
          server.write(new Message({ username, command,joiner }).toJSON() + '\n')
           previousCommand=command


        }

        else {
          if(previousCommand===''){

            this.log(chalk.green(`Command <${command}> was not recognized`))
          }
          else{
            hasCommand= true
            //if the command was not recognized it is because the user forgot to type
            // the a command word. there fore the command element was ment to be part of the contents
            contents=command+' '+contents
            command= previousCommand

          }
        }
    } while(hasCommand)

    callback()
  })
