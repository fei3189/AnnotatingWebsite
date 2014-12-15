package models

import java.sql.DriverManager
import java.sql.Connection
import scala.io.Source;
import scala.util.control.Breaks._

object Next {
    val driver = "com.mysql.jdbc.Driver"
    val url = "jdbc:mysql://localhost/textimage2"
    val username = "root"
    val password = "thuir2013"
    
    Class.forName(driver)
    
    
    def getuid(tid: Long): Long = {
    	val connection = DriverManager.getConnection(url, username, password)
    	val statement = connection.createStatement()
		val sql = "SELECT uid FROM task WHERE tid = " + tid
		val result = statement.executeQuery(sql)
		var ret = 0L
		if (result.next()) {
			ret = result.getLong("uid")
		}
    	statement.close
    	connection.close
    	return ret
    }
    
    def nextExampleWeibo(uid: Long): Example = {
    	val connection = DriverManager.getConnection(url, username, password)
    	var statement = connection.createStatement()
    	var sql = "SELECT exp_f FROM user WHERE uid = " + uid
    	var result = statement.executeQuery(sql)
    	var seq = 0
		if (result.next()) {
			seq = result.getInt("exp_f")
		} else {
			statement.close
			connection.close
			return null
		}
    	seq += 1
    	if (seq > 20) {
    		statement.close
			connection.close
    		return null
    	}
    	
    	sql = "UPDATE user SET exp_f = "  + seq + " WHERE uid = " + uid
    	statement.execute(sql)
    	
    	sql = "SELECT mid, ltext, lwhole, revelant, reason, imagereason from training WHERE seq = " + seq
    	result = statement.executeQuery(sql)
    	if (result.next()) {
			val ret = new Example(result.getLong("mid"), result.getInt("ltext"), result.getInt("lwhole"), seq, result.getInt("revelant"), result.getString("reason"), result.getString("imagereason"))
			statement.close
			connection.close
			return ret
    	} else {
			statement.close
			connection.close
			return null
		}
    }
    
	def valid(tid: Long): Task = {
		val connection = DriverManager.getConnection(url, username, password)
    	val statement = connection.createStatement()
		val sql = "SELECT tid, finished, number FROM task WHERE tid = " + tid
		val result = statement.executeQuery(sql)
		if (result.next()) {
			val ret  =  new Task(tid, result.getInt("finished"), result.getInt("number"))
			statement.close
			connection.close
			return ret
		} else {
			statement.close
			connection.close
			return null
		}
	}
    
    def list(tid: Long): Array[Weibo] = {
    	val connection = DriverManager.getConnection(url, username, password)
    	var wlist = new Array[Weibo](0)
    	val statement = connection.createStatement()
    	val sql = "SELECT mid, ltext, lwhole, relevant FROM label WHERE labeled = 1 AND tid = " + tid + " ORDER BY seq ASC"
    	val result = statement.executeQuery(sql)
    	while (result.next()) {
    		wlist = wlist :+ new Weibo(result.getLong("mid"), "", "", result.getInt("ltext"), result.getInt("lwhole"), result.getInt("relevant"))
    	}
    	for(i <- 0 until wlist.length) {
    		val sql = "SELECT text, image FROM weibo WHERE mid = " + wlist(i).mid
    		val result = statement.executeQuery(sql)
    		if (result.next()) {
    			wlist(i).text = result.getString("text")
    			wlist(i).image = result.getString("image")
    		}
    	}
    	statement.close
    	connection.close
    	return wlist
    }
    
    def next(tid: Long): Long = {
    	val connection = DriverManager.getConnection(url, username, password)
    	val statement = connection.createStatement()
    	val sql = "SELECT mid FROM label WHERE tid = " + tid + " AND labeled = 0 LIMIT 1"
    	val result = statement.executeQuery(sql)
    	if (result.next()) {
    		val ret =  result.getLong("mid")
    		statement.close
    		connection.close
    		return ret
    	} else {
    		statement.close
    		connection.close
    		return 0L  // No more unlabeled data in this task
    	}
    }
    
    def getWeibo(mid: Long): Weibo = {
    	val connection = DriverManager.getConnection(url, username, password)
    	val statement = connection.createStatement()
    	val sql = "SELECT mid, text, image FROM weibo WHERE mid = " + mid
    	val result = statement.executeQuery(sql)
    	if (result.next()) {
    		val ret = new Weibo(mid, result.getString("text"), result.getString("image"))
    		statement.close
    		connection.close
    		return ret
    	} else {
    		val ret = new Weibo(0, "", "")
    		statement.close
    		connection.close
    		return ret
    	}
    }
    
    def update(tid: Long, mid: Long, tscore: Int, ascore: Int, relevant: Int): Int = {
    	val connection = DriverManager.getConnection(url, username, password)
    	val statement = connection.createStatement()
    	var sql = "SELECT finished FROM task WHERE tid = " + tid
    	var result = statement.executeQuery(sql)
    	var finished = 0
    	var seq = 0
    	var labeled = 0
    	if (result.next()) {
    		finished = result.getInt("finished")
    	} else {
    		statement.close
    		connection.close
    		return -1
    	}
    	sql = "SELECT labeled, seq FROM label WHERE tid = " + tid + " AND mid = " + mid
    	result = statement.executeQuery(sql)
    	if (result.next()) {
    		labeled = result.getInt("labeled")
    		seq = result.getInt("seq")
    	} else {
    		statement.close
    		connection.close
    		return -1
    	}
    	if (labeled == 0) {
    		labeled = 1
    		finished += 1
    		seq = finished
    		sql = "UPDATE task SET finished = " + finished + " WHERE tid = " + tid
    		if (statement.executeUpdate(sql) != 1) {
    			statement.close
    			connection.close
    			return -3
    		}
    	}
    	sql = "UPDATE label SET ltext = " + tscore + ", lwhole = " + ascore + ", relevant = " + relevant + ", seq = " + seq + ", labeled = 1 WHERE tid = " + tid + " AND mid = " + mid
    	
    	if (statement.executeUpdate(sql) != 1) {
    		statement.close
    		connection.close
    		return -4
    	}
    	statement.close
    	connection.close
    	return 0
    }
}