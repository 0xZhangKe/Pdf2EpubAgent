package com.zhangke.agent.pdf2epub.framework

import com.zhangke.agent.pdf2epub.framework.http.sharedHttpClient
import io.ktor.client.HttpClient
import org.koin.dsl.module

val frameworkKoinModule = module {
    single<HttpClient> { sharedHttpClient }
}
