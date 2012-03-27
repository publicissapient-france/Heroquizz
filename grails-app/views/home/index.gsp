<head>
    <meta name="layout" content="main"/>
    <title><g:message code="app.title"/></title>
</head>

<body>
<div class="row">

    <div class="span3">

        <div class="well" id="facebook-connect">
            <g:render template="/commons/facebook-button"/>
        </div>

        <div id="facebook-like" style="display: none;">
            <div class="well">
                <div class="fb-like"
                     data-href="<g:createLink controller="home" absolute="true"/>"
                     data-send="false"
                     data-width="200" data-show-faces="true"></div>

            </div>

            <div class="well">
                <div class="fb-subscribe"
                     data-href="<g:createLink controller="home" absolute="true"/>"
                     data-show-faces="true"
                     data-width="200"></div>
            </div>
        </div>

    </div>

    <div class="span8 well">

        <h1><g:message code="index.welcome"/></h1>

        <hr/>

        <p>
            Testez votre niveau en Java et vos connaissances agiles.
        </p>

        <p>
            Choisissez votre QCM parmi ceux qui vous sont proposés.
            Visionnez les questions en vidéo, et cochez la ou les bonnes réponses
        </p>

        <p>
            Votre score s'affichera sur votre mur Facebook et vous donnera votre niveau technique et agile.
        </p>

        <p>
            Challengez vos amis en leur envoyant un Xebia Quizz.
        </p>

        <hr/>

        <div class="ctxt">
            <g:link class="btn btn-primary" controller="quizz" action="index"><g:message
                    code="index.enter.quizz"/></g:link>
        </div>

    </div>
</div>
</body>