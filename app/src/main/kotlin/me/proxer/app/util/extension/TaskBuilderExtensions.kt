package me.proxer.app.util.extension

import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.ProxerTask

fun <O> TaskBuilder.Companion.proxerTask() = TaskBuilder.task(ProxerTask<O>())
