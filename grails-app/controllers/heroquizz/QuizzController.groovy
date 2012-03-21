package heroquizz

import org.springframework.dao.DataIntegrityViolationException
import grails.plugins.springsecurity.Secured
import org.springframework.social.facebook.api.impl.FacebookTemplate
import org.springframework.social.facebook.api.FacebookLink
import org.codehaus.groovy.grails.web.mapping.LinkGenerator

@Secured(['ROLE_ADMIN', 'ROLE_FACEBOOK'])
class QuizzController {

  def springSecurityService

  LinkGenerator grailsLinkGenerator

  static allowedMethods = [save: "POST", update: "POST", delete: "POST"]

  def index() {}

  @Secured('ROLE_ADMIN')
  def list() {
    params.max = Math.min(params.max ? params.int('max') : 10, 100)
    [quizzInstanceList: Quizz.list(params), quizzInstanceTotal: Quizz.count()]
  }

  @Secured('ROLE_ADMIN')
  def create() {
    [quizzInstance: new Quizz(params)]
  }

  @Secured('ROLE_ADMIN')
  def save() {
    def quizzInstance = new Quizz(params)
    if (!quizzInstance.save(flush: true)) {
      render(view: "create", model: [quizzInstance: quizzInstance])
      return
    }

    flash.message = message(code: 'default.created.message', args: [message(code: 'quizz.label', default: 'Quizz'), quizzInstance.id])
    redirect(action: "show", id: quizzInstance.id)
  }

  @Secured('ROLE_ADMIN')
  def show() {
    def quizzInstance = Quizz.get(params.id)
    if (!quizzInstance) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "list")
      return
    }

    [quizzInstance: quizzInstance]
  }

  @Secured('ROLE_ADMIN')
  def edit() {
    def quizzInstance = Quizz.get(params.id)
    if (!quizzInstance) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "list")
      return
    }

    [quizzInstance: quizzInstance]
  }

  @Secured('ROLE_ADMIN')
  def update() {
    def quizzInstance = Quizz.get(params.id)
    if (!quizzInstance) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "list")
      return
    }

    if (params.version) {
      def version = params.version.toLong()
      if (quizzInstance.version > version) {
        quizzInstance.errors.rejectValue("version", "default.optimistic.locking.failure",
            [message(code: 'quizz.label', default: 'Quizz')] as Object[],
            "Another user has updated this Quizz while you were editing")
        render(view: "edit", model: [quizzInstance: quizzInstance])
        return
      }
    }

    quizzInstance.properties = params

    if (!quizzInstance.save(flush: true)) {
      render(view: "edit", model: [quizzInstance: quizzInstance])
      return
    }

    flash.message = message(code: 'default.updated.message', args: [message(code: 'quizz.label', default: 'Quizz'), quizzInstance.id])
    redirect(action: "show", id: quizzInstance.id)
  }

  @Secured('ROLE_ADMIN')
  def delete() {
    def quizzInstance = Quizz.get(params.id)
    if (!quizzInstance) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "list")
      return
    }

    try {
      quizzInstance.delete(flush: true)
      flash.message = message(code: 'default.deleted.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "list")
    }
    catch (DataIntegrityViolationException e) {
      flash.message = message(code: 'default.not.deleted.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      redirect(action: "show", id: params.id)
    }
  }

  def take() {
    Quizz theQuizz = Quizz.get(params.id)
    if (!theQuizz) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      return redirect(action: 'index')
    }

    FacebookUser loggedUser = FacebookUser.findByUser(springSecurityService.getPrincipal())
    def currentQuizzAnswer = QuizzAnswer.findByOwnerAndOriginalQuizz(loggedUser, theQuizz)

    if (!currentQuizzAnswer) {
      currentQuizzAnswer = new QuizzAnswer(originalQuizz: theQuizz, score: 0, completed: false, owner: loggedUser).save()
      loggedUser.addToQuizzAnswers(currentQuizzAnswer).save()
    }

    def questionList = theQuizz.questions as List
    def nextQuestion = questionList.find { !currentQuizzAnswer.validated?.contains(it) }

    if (!nextQuestion) {
      currentQuizzAnswer.completed = true
      currentQuizzAnswer.save()
      return redirect(action: 'passed', id: params.id)
    }

    [quizzInstance: theQuizz, questionInstance: nextQuestion]
  }



  def answer() {
    Question currentQuestion = Question.get(params.id as Long)

    Quizz theQuizz = Quizz.get(params.quizzId)
    if (!theQuizz) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.quizzId])
      return redirect(action: 'index')
    }

    FacebookUser loggedUser = FacebookUser.findByUser(springSecurityService.getPrincipal())
    def currentQuizzAnswer = QuizzAnswer.findByOwnerAndOriginalQuizz(loggedUser, theQuizz)

    if (!currentQuizzAnswer) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.quizzId])
      return redirect(action: 'index')
    }


    def answerList = []
    answerList << params.answers
    answerList = answerList.flatten()

    answerList.each { userAnswer ->
      def matchedAnswer = currentQuestion.answers.find { it.id == userAnswer as Long}
      if (matchedAnswer) {
        currentQuizzAnswer.score += matchedAnswer.pointsNumber
      }
    }

    currentQuizzAnswer.addToValidated(currentQuestion)

    currentQuizzAnswer.save()

    println "Score is now: ${currentQuizzAnswer.score}"

    redirect(action: 'take', id: params.quizzId)
  }

  def passed() {
    Quizz theQuizz = Quizz.get(params.id)
    if (!theQuizz) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.id])
      return redirect(action: 'index')
    }

    FacebookUser loggedUser = FacebookUser.findByUser(springSecurityService.getPrincipal())
    def currentQuizzAnswer = QuizzAnswer.findByOwnerAndOriginalQuizz(loggedUser, theQuizz)

    if (!currentQuizzAnswer) {
      flash.message = message(code: 'default.not.found.message', args: [message(code: 'quizz.label', default: 'Quizz'), params.quizzId])
      return redirect(action: 'index')
    }

    FacebookTemplate facebook = new FacebookTemplate(loggedUser.accessToken)
    def postContent = """
      Au questionnaire HeroQuizz ${currentQuizzAnswer.originalQuizz.name}, j'ai fait un score de
      ${currentQuizzAnswer.score}. Essaie un peu de faire mieux pour voir !
    """
    try {
      def quizzUrl = grailsLinkGenerator.link(controller: 'quizz', action: 'take', id: currentQuizzAnswer.originalQuizz.id, absolute: true)
      log.debug("quizzUrl : ${quizzUrl}")
      facebook.feedOperations().postLink('me', postContent.toString(), new FacebookLink(quizzUrl, "Essayer", "caption", "Questionnaire"))

    } catch (Exception e) {
      log.error("aaah", e)
    }
    [quizzAnswerInstance: currentQuizzAnswer]
  }
}
