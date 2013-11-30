import cgi
from user import *
import httplib
import json
import urllib
import urllib2
import urlparse
from google.appengine.ext import db


from google.appengine.api import users
from google.appengine.ext import ndb
import webapp2


API_KEY = "AIzaSyBXJmrlHISOeug-BV-1KMLIWGJtcjdPKxQ"


class InChat(ndb.Model):
    username = ndb.StringProperty(indexed = True)
    interlocutor = ndb.StringProperty(indexed=True)


class Message(ndb.Model):
    sender = ndb.StringProperty(indexed = True)
    receiver = ndb.StringProperty(indexed=True)
    receiver_regid = ndb.StringProperty(indexed = True)
    content = ndb.StringProperty(indexed=False)
    msgNo = ndb.IntegerProperty(indexed = True)
    server_timestamp = ndb.DateTimeProperty(auto_now_add=True)


class Utils(webapp2.RequestHandler):

    def gotPreviousMsg (self, msgNo, receiver, sender):

        if(msgNo == 1):
            return True

        message = Message.query(Message.sender == sender, Message.receiver == receiver, Message.msgNo == msgNo - 1).get()

        if message:
            print "okokokokokokokokokokokok"
            return True

        print "not saved!!!!!!!!!!!!!!!!!!!!!!!!"
        return False





class MessageReceiver(webapp2.RequestHandler):

    def notify(self, receiver_regid, sender):

        print "sender: " + sender

        global API_KEY

        data = {
                'registration_ids' : [receiver_regid],
                'data' : {
                          'sender' : sender
                        }
        }

        headers = {
          'Content-Type' : 'application/json',
          'Authorization' : 'key=' + API_KEY
        }

        url = 'https://android.googleapis.com/gcm/send'
        request = urllib2.Request(url, json.dumps(data), headers)
        try:
            response = urllib2.urlopen(request)
            returned_stuff = json.load(response)
            results = returned_stuff["results"][0]

            print returned_stuff
            return

        except urllib2.HTTPError, e:
            print "me has errors"
            print e.code
            print e.read()



    def notifyDataLost(self, sender_regid, receiver, received_msgs):

        global API_KEY

        data = {
                'registration_ids' : [sender_regid],
                'data' : {
                          'lost_receiver' : receiver,
                          'recieved_msgs':received_msgs
                        }
        }

        headers = {
          'Content-Type' : 'application/json',
          'Authorization' : 'key=' + API_KEY
        }

        url = 'https://android.googleapis.com/gcm/send'
        request = urllib2.Request(url, json.dumps(data), headers)
        try:
            response = urllib2.urlopen(request)
            returned_stuff = json.load(response)
            results = returned_stuff["results"][0]

            print returned_stuff
            return

        except urllib2.HTTPError, e:
            print "me has errors"
            print e.code
            print e.read()


    def post(self):

        receiver = self.request.get('receiver')
        message = self.request.get('content')
        sender = self.request.get('sender')
        msgNo_sent = self.request.get('msgNo_sent')
        msgNo_received = self.request.get('msgNo_received')

        query = "username: " + receiver
        users_in_DB = userIndex.search(query)

        utils = Utils()
        if not users_in_DB:
            return 404
        for user_in_DB in users_in_DB:

            if(utils.gotPreviousMsg (int(msgNo_sent), receiver, sender)):
                receiver_regid = user_in_DB.fields[1].value
                msg = Message()
                msg.receiver = receiver
                msg.sender = sender
                msg.content = message
                msg.msgNo = int(msgNo_sent)
                msg.put()

                inChat = InChat.query(InChat.username == sender, InChat.interlocutor == receiver).get()
                if not inChat:
                    print "============= out of chat =============="
                    self.notify(receiver_regid, sender)
                else:
                    print "================ in chat ============"
                    ms = MessageSender()
                    ms.getDataFromDB(sender, receiver, msgNo_received, msg)


            else:
                messages = Message.query(Message.sender == sender, Message.receiver == receiver).fetch()
                maximum = 0;
                for message in messages:
                    if maximum<message.msgNo:
                        maximum = message.msgNo

                query = "username: " + sender
                users_in_DB = userIndex.search(query)
                for user_in_DB in users_in_DB:
                    sender_regid = user_in_DB.fields[1].value

                self.notifyDataLost(sender_regid, receiver, maximum)

                break



class MessageSender(webapp2.RequestHandler):

    def send_messages(self, receiver_regid, sender, receiver, msgNo, msg):

        print "================================"
        print sender
        print receiver
        print msgNo
        print "================================"

        #TODO
        messages = Message.query(Message.sender == sender, Message.receiver == receiver).fetch(10)

        if not messages:
            print "Oooopsy, me has no messages for you."

        response = '{"response": ['
        for message in messages:
            print message.msgNo
            print msgNo
            if (int(message.msgNo) < int(msgNo)):
                print "deleted %d"%int(msgNo)
                message.key.delete()
            else:
                response += '{"sender":\"%s\", "msgNo": \"%d\", "content": \"%s\"},'%(sender,message.msgNo,message.content)

        if msg:
            response += '{"sender":\"%s\", "msgNo": \"%d\", "content": \"%s\"},'%(sender,msg.msgNo,msg.content)

        if len(response)>len('{"response": ['):
            aux = response[:-1]
            response = aux;
        response += ']}'

        print response

        global API_KEY

        data = {
            'registration_ids' : [
                receiver_regid
             ],
            'data' : {
                'messages' : response
            }
        }

        headers = {
          'Content-Type' : 'application/json',
          'Authorization' : 'key=' + API_KEY
        }

        url = 'https://android.googleapis.com/gcm/send'
        request = urllib2.Request(url, json.dumps(data), headers)
        try:
            response = urllib2.urlopen(request)
            return
        except urllib2.HTTPError, e:
            print "me has errors"
            print e.code
            print e.read()




    def getDataFromDB(self, sender, receiver, msgNo, msg):
        print "sending msgs.............."
        query = 'username: ' + receiver
        users_in_DB = userIndex.search(query)
        for user_in_DB in users_in_DB:
            receiver_regid = user_in_DB.fields[1].value
            self.send_messages(receiver_regid, sender, receiver, msgNo, msg);
        print receiver_regid



    def post(self):
        sender = self.request.get('sender')
        receiver = self.request.get('receiver')
        msgNo_sent = self.request.get('msgNo_sent')
        msgNo_received = self.request.get('msgNo_received')
        self.getDataFromDB(sender, receiver, msgNo_sent, msgNo_received)




class InOutOfChatNotifier(webapp2.RequestHandler):

    def post(self):

        _inChat = self.request.get('inChat')
        username = self.request.get('username')
        interlocutor = self.request.get('interlocutor')
        msgNo= self.request.get('msgNo')
        print "this MF sends meh:"
        print msgNo


        if(_inChat == "true"):
            print "!!!!!!!!!!!!!!!!chat activated!!!!!!!!!!!!!!!!!!!"
            inChat = InChat()
            inChat.username = username
            inChat.interlocutor = interlocutor
            inChat.put()
        else:
            print "!!!!!!!!!!!!!!!!chat deactivated!!!!!!!!!!!!!!!!!"
            inChat = InChat.query(InChat.username == username, InChat.interlocutor == interlocutor).get()
            inChat.key.delete()




class GetUnreceivedMessages(webapp2.RequestHandler):
    def post(self):
        receiver = self.request.get('username')
        messages = Message.query(Message.receiver == receiver).order(Message.msgNo)
        self.response.write('{"results": [')
        for message in messages:
            self.response.write('{"sender":\"%s\", "msgNo": \"%d\", "content": \"%s\"},'%(message.sender,message.msgNo,message.content))
            message.key.delete()
        self.response.write(']}')
