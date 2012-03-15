package heroquizz

import heroquizz.User
import com.the6hours.grails.springsecurity.facebook.FacebookUserDomain

class FacebookUser implements FacebookUserDomain {

  long uid
  String accessToken

  String email
  String firstName
  String lastName
  
  static belongsTo = [user: User]

  static constraints = {
    uid unique: true
  }
}
