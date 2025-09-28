/*
 * Copyright (c) 2017. tangzx(love.tangzx@qq.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.tang.intellij.lua.debugger.emmy
import com.tang.intellij.lua.debugger.*
import com.tang.intellij.lua.psi.LuaFileManager
import com.tang.intellij.lua.psi.LuaFileUtil
import com.tang.intellij.lua.project.LuaSourceRootManager
import java.io.File
import com.tang.intellij.lua.debugger.LogConsoleType
import com.intellij.execution.ui.ConsoleViewContentType
import com.intellij.xdebugger.XDebugSession

interface IEvalResultHandler {
    fun handleMessage(msg: EvalRsp)
}

open class EmmyDebugProcess(session: XDebugSession) : EmmyDebugProcessBase(session), ITransportHandler {
    private val configuration = session.runProfile as EmmyDebugConfiguration

    override fun setupTransporter() {
        val transporter: Transporter = when (configuration.type) {
            EmmyDebugTransportType.PIPE_CLIENT -> PipelineClientTransporter(configuration.pipeName)
            EmmyDebugTransportType.PIPE_SERVER -> PipelineServerTransporter(configuration.pipeName)
            EmmyDebugTransportType.TCP_CLIENT -> SocketClientTransporter(configuration.host, configuration.port)
            EmmyDebugTransportType.TCP_SERVER -> SocketServerTransporter(configuration.host, configuration.port)
        }
        transporter.handler = this
        transporter.logger = this
        this.transporter = transporter
        try {
            transporter.start()
        } catch (e: Exception) {
            this.error(e.localizedMessage)
            this.onDisconnect()
        }
    }

    override fun sendHotfix()
    {
        var hotfixList = configuration.hotfixList
        val lines: List<String> = hotfixList.split(Regex("\r\n|\r|\n"))
        val roots = LuaSourceRootManager.getInstance(session.project).getSourceRoots()
        println("Try send hotfix files:$hotfixList", LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
        for (root in roots) {
            println("Root:$root", LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
            for (line in lines) {
                var fullPath = root.url + "/" + line
                var file = File(fullPath)
                if (file.exists()) {
                    println("Send Hotfix:$fullPath", LogConsoleType.NORMAL, ConsoleViewContentType.SYSTEM_OUTPUT)
                    transporter.send(HotfixMessage(line, file.readText()))
                }
            }
        }
    }
}