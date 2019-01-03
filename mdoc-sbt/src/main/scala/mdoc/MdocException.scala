package mdoc

import sbt.internal.util.FeedbackProvidedException

case class MdocException(message: String)
    extends Exception(message: String)
    with FeedbackProvidedException
