package io.github.qwefgh90.repogarden.bp.github

import scala.collection.JavaConverters._

import org.eclipse.egit.github.core._
import org.eclipse.egit.github.core.service._

object Implicits {
	class ContentsServiceEx(contentsService: ContentsService){
	  /**
	   * @param repoProvider repository provider
	   * @param path if path is null, iterate contents from root. Otherwise, iterate contents from path
	   * @param recursive whether to iterate all sub directories
	   * @return a list of contents     
	   */
		def getContents(repoProvider: IRepositoryIdProvider, path: String, recursive: Boolean): List[RepositoryContents] = {
			val contentList = contentsService.getContents(repoProvider, path).asScala.toList
			contentList.flatMap{content => 
			  if(content.getType == "dir") 
			    getContents(repoProvider, content.getPath, true) 
			  else 
			    List(content) 
			}
		}
	}
	
	/**
	 * A converter from ContentsService to ContentsServiceEx
	 */
	implicit def extendContentsService(service: ContentsService) = new ContentsServiceEx(service)
}