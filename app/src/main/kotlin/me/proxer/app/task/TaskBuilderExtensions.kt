package me.proxer.app.task

import com.rubengees.ktask.util.TaskBuilder

fun <O> TaskBuilder.Companion.proxerTask() = task(ProxerTask<O>())
fun <O> TaskBuilder.Companion.asyncProxerTask() = task(ProxerTask<O>()).async()
