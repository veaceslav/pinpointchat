from user import *
from gcm_server import *
import webapp2

application = webapp2.WSGIApplication([
    ('/add', AddUser),
    ('/getusers',GetUsers),
    ('/delete',DeleteUser),
    ('/register', SignIn),
    ('/send_msg', MessageReceiver),
    ('/fetch_msgs',MessageSender),
    ('/updatelocation',UpdateLocation),
    ('/in_out_chat', InOutOfChatNotifier),
    ('/getunreceivedmessages',GetUnreceivedMessages),
    ('/',MainPage)
], debug=True)
