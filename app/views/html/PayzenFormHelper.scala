package views.html

import play.twirl.api.Html

/**
 * @author Valentin Kasas
 */
object PayzenFormHelper {

  def formFields(data: Map[String,String]): Html = {
    val innerStr = data.toList.foldLeft(""){
      (str, keyvalue) => str + s"""<input type="hidden" name="${keyvalue._1}" value="${keyvalue._2}"/>"""
    }
    Html(innerStr)
  }

}
