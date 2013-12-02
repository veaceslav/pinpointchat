import cgi
import math
import urllib
import webapp2
from google.appengine.api import search
from google.appengine.api import users

DEFAULT_USER = 'default_user'
DEFAULT_LINK = ""
DEFAULT_VALUE = 0
DEFAULT_GEOPOINT = search.GeoPoint(0, 0)
INDEX_NAME = "UserIndex"

def CreateUserIndexDocument(name, latitude, longitude, regid, doc_id = "0"):
    geopoint = search.GeoPoint(latitude, longitude)
    if doc_id =="0":
        return search.Document(
            fields =
            [
                search.TextField(name='username', value=name),
                search.TextField(name= 'regid', value = regid),
                search.GeoField(name='position', value=geopoint)
            ]
        )
    else:
        return search.Document(doc_id = doc_id,
            fields =
            [
                search.TextField(name='username', value=name),
                search.TextField(name= 'regid', value = regid),
                search.GeoField(name='position', value=geopoint)
            ]
        )

userIndex = search.Index(name = INDEX_NAME)

################################################################################################################

# A nu se sterge (momentan) MAIN_PAGE_FOOTER_TEMPLATE, MainPage si AddUser
# sunt folosite in verificarea corectitudinii

MAIN_PAGE_FOOTER_TEMPLATE = """\
    <form action="/add?%s" method="post">Guestbook name:
      <input value="name here" name="username">
      <input value="latitude here" name="userlatitude" type = "float">
      <input value="longitude here" name="userlongitude" type = "float">
      <input type="submit" value="Send" method=>
    </form>
  </body>
</html>
"""

class MainPage(webapp2.RequestHandler):

    def get(self):
        self.response.write('<html><body>')
        name = self.request.get('username',DEFAULT_USER)
        self.response.write(MAIN_PAGE_FOOTER_TEMPLATE)


        try:
            query = "NOT (username: troptrop) OR (username: troptrop)"

            results = userIndex.search(query)
            for result in results:
                self.response.write('<b>%s</b> ' %  result.fields[0].value)
                self.response.write('<b>%s</b> ' %  result.fields[2].value.latitude)
                self.response.write('<b>%s</b> ' %  result.fields[2].value.longitude)
                self.response.write('<p></p>')
        except search.Error:
            logging.exception('Search failed')


class AddUser(webapp2.RequestHandler): #adaugare user in baza de date

    def post(self):
        username = self.request.get('username',DEFAULT_USER)
        latitude = (float)(self.request.get('userlatitude',DEFAULT_VALUE))
        longitude = (float)(self.request.get('userlongitude',DEFAULT_VALUE))
        regid = "regid44"
        userIndex.put(CreateUserIndexDocument(username,latitude,longitude,regid))

        self.redirect('/')

def haversine(origin, destination):

    lat1, lon1 = origin
    lat2, lon2 = destination
    radius = 6371

    dlat = math.radians(lat2-lat1)
    dlon = math.radians(lon2-lon1)
    a = math.sin(dlat/2) * math.sin(dlat/2) + math.cos(math.radians(lat1)) \
        * math.cos(math.radians(lat2)) * math.sin(dlon/2) * math.sin(dlon/2)
    c = 2 * math.atan2(math.sqrt(a), math.sqrt(1-a))
    d = radius * c

    return d

################################################################################################################


class SignIn(webapp2.RequestHandler):
    def post(self):
        username = self.request.get('username')
        regid = self.request.get('regid')
        query = ("username: " + username)
        user_in_DB = userIndex.search(query)

        for usertemp in user_in_DB:
            return self.error(406) #406 = not accepted

        userIndex.put(CreateUserIndexDocument(username,DEFAULT_VALUE,DEFAULT_VALUE,regid))


class DeleteUser(webapp2.RequestHandler):
    def post(self):
        username = self.request.get('username')

        query = ("username: " + username)
        user_in_DB = userIndex.search(query)

        for usertemp in user_in_DB:
            delete_document_id = usertemp.doc_id
            userIndex.delete(delete_document_id)



class GetUsers(webapp2.RequestHandler):

    def post(self):
        username = self.request.get('username',DEFAULT_USER)

        query = ("username: " + username)
        users_query = userIndex.search(query)

        for MainUser in users_query:
            query = "" #"distance(GeoPoint, geopoint(-33.857, 151.215)) < 4500"
            users = userIndex.search(query)
            c2 = [MainUser.fields[2].value.latitude,MainUser.fields[2].value.longitude]

            self.response.write ('{"results": [')
            for user in users: #am format manual JSONul
                c1 = [user.fields[2].value.latitude,user.fields[2].value.longitude]
                distance = haversine(c1, c2) * 1000
                self.response.write("{ \"username\": \"%s\", \"distance\": \"%s\"," %(user.fields[0].value, distance))
                self.response.write(" \"lat\": \"%s\", \"lng\": \"%s\" }," %(c1[0], c1[1]))
            self.response.write(']}')

        print self.response


class UpdateLocation(webapp2.RequestHandler):
    def post(self):
        username = self.request.get('username',DEFAULT_USER)
        latitude = (float)(self.request.get('latitude',DEFAULT_VALUE))
        longitude = (float)(self.request.get('longitude',DEFAULT_VALUE))

        query = ("username: " + username)
        users_query = userIndex.search(query)

        for mainUser in users_query:
            regid = mainUser.fields[1].value
            doc_id = mainUser.doc_id
            updatedMainUser = CreateUserIndexDocument(username, latitude, longitude, regid,doc_id)
            userIndex.put(updatedMainUser)


