package com.ansvia.digapidoc.parser

import java.io._
import com.ansvia.commons.logging.Slf4jLogger

/**
 * Author: robin
 * Date: 10/11/13
 * Time: 12:39 PM
 *
 */
object FileParser extends Slf4jLogger {

    def parse(file:String, includeSymbols:Map[String,String]):Seq[DocBase] =
        parse(new File(file), includeSymbols)

    def parse(file:File, includeSymbols:Map[String,String]):Seq[DocBase] = {
        parse(new FileInputStream(file), file.getName, includeSymbols)
    }

    def parse(fileIs:InputStream, fileName:String,
              includeSymbols:Map[String,String]):Seq[DocBase] = {

        val isr = new InputStreamReader(fileIs)
        val bfr = new BufferedReader(isr)

        var docs = Seq.newBuilder[DocBase]
        
        var currentDocGroup:DocGroup = null
        
        var line = ""
        do {
            line = bfr.readLine()

            if (line != null){
                if (Doc.stripIdent(line).startsWith("/**")){
                    line = line + "\n" + bfr.readLine()
                }
                if (Doc.isHeaderValid(line)){
                    val sb = StringBuilder.newBuilder
                    line = line + "\n"
                    do {
                        sb.append(Doc.stripIdent(line))
                        line = bfr.readLine() + "\n"
                    }while(line != null && !line.contains("*/"))
                    val textRaw = sb.result().trim + "\n*/"
                    
                    val doc = Doc.parse(textRaw, fileName, includeSymbols)
                    
                    doc match {
                        case dg:DocGroup =>
                            currentDocGroup = dg
                            docs ++= Seq(currentDocGroup)
                            
//                        case d:Doc if docGroups.contains(d) =>
                        case d:Doc =>
                            info("processing: " + d)
                            if (currentDocGroup != null)
                                currentDocGroup.docs ++= Seq(d)
                            else
                                docs += d
                    }
                    
                    
                }
            }

        }while(line != null)

        bfr.close()
        isr.close()

        docs.result()
    }

    def scan(dir:String, includeSymbols:Map[String,String]):Seq[DocBase] =
        scan(new File(dir), includeSymbols)

    def scan(dir:File, includeSymbols:Map[String,String]):Seq[DocBase] = {

        if (!dir.exists())
            throw new NotExists("Directory not exists: " + dir.getAbsolutePath)

        if (!dir.isDirectory)
            throw new InvalidParameter(dir.getAbsolutePath + " is not directory")

        var rv = Seq.newBuilder[DocBase]

        dir.listFiles().foreach { f =>

            if (f.isDirectory){
                rv ++= scan(f, includeSymbols)

            }else{

                debug("processing: " + f)
                rv ++= parse(f, includeSymbols)

            }
        }

        rv.result()
    }
}
