package controllers

import play.api._
import play.api.mvc._
import play.api.Logger
import models.Task
import models.Next
import models.Weibo
import play.api.data._
import play.api.data.Forms._

object Application extends Controller {
	def login = Action {
		Ok(views.html.login())
	}

	def list = Action {
		request =>
			{
				Logger.info(" List " + request.remoteAddress + ": " + request.rawQueryString)
				val parameters = request.queryString.map { case (k, v) => k -> v.mkString };
				val taskid = parameters("taskid").toLong
				val items = Next.list(taskid)
				Ok(views.html.list(items, taskid))
			}
	}
	
	def example = Action {
		request =>
			{
				val parameters = request.queryString.map { case (k, v) => k -> v.mkString };
				val taskid = parameters("taskid").toLong
				val uid = Next.getuid(taskid)
				if (uid > 0) {
					val nextmid = Next.nextExampleWeibo(uid)
					if (nextmid == null)
						Redirect("labelpage?taskid=" + taskid)
					else {
						val weibo = Next.getWeibo(nextmid.mid)
						println(nextmid.mid)
						if (weibo.mid > 0) {
							Ok(views.html.example(taskid, weibo.mid, nextmid.seq, 20, weibo.text, weibo.image, nextmid.ltext, nextmid.lwhole, nextmid.relevant, nextmid.reason, nextmid.imagereason))
						} else {
							Ok("微博不存在")
						}
					}
				} else {
					Redirect("labelpage?taskid=" + taskid)
				}
			}
	}

	def labelPage = Action {
		request =>
			{
				Logger.info(" LabelPage " + request.remoteAddress + ": " + request.rawQueryString)
				val parameters = request.queryString.map { case (k, v) => k -> v.mkString };
				val taskid = if (parameters contains "taskid") parameters("taskid").toLong else 0L
				val task = if (taskid != 0L) Next.valid(taskid) else null
				val mid = if (parameters contains "mid") parameters("mid").toLong else 0L
				if (task != null) {
					var next = 0L
					if (parameters contains "mid") {
						next = parameters("mid").toLong
						val weibo = Next.getWeibo(next)
						if (weibo.mid > 0) {
							Ok(views.html.label(taskid, weibo.mid, task.finished, task.total, weibo.text, weibo.image))
						} else {
							Ok("微博" + next + "不存在")
						}
					} else {
						next = Next.next(taskid)
						if (next > 0) {
							val weibo = Next.getWeibo(next)
							Ok(views.html.label(taskid, weibo.mid, task.finished, task.total, weibo.text, weibo.image))
						} else {
							Ok(views.html.finish(taskid))
						}
					}
				} else if (mid != 0L) {
					val weibo = Next.getWeibo(mid)
					if (weibo.mid > 0) {
						Ok(views.html.label(0, weibo.mid, 0, 0, weibo.text, weibo.image))
					} else {
						Ok("微博" + weibo.mid + "不存在")
					}
				}  else {
					Ok("任务号" + taskid + "不存在")
				}
			}
	}

	def labeling = Action {
		request =>
			{
				Logger.info(" Labeling " + request.remoteAddress + ": " + request.rawQueryString)
				val parameters = request.queryString.map { case (k, v) => k -> v.mkString };
				val taskid = parameters("taskid").toLong
				val mid = parameters("mid").toLong
				val tscore = parameters("tscore").toInt
				val ascore = parameters("ascore").toInt
				val relevant = parameters("relevant").toInt
				val res = Next.update(taskid, mid, tscore, ascore, relevant)
				if (res == 0)
					Redirect("labelpage?taskid=" + taskid)
				else
					Ok("系统写入错误！")
			}
	}
}