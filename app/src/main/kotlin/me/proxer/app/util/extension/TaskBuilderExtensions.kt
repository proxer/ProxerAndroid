package me.proxer.app.util.extension

import com.rubengees.ktask.base.Task
import com.rubengees.ktask.util.TaskBuilder
import me.proxer.app.task.PagedTask
import me.proxer.app.task.ProxerTask

fun <I, O, T : Task<I, O>> TaskBuilder<I, O, T>.proxer() = TaskBuilder.task(ProxerTask<O>())

fun <I, O, T : Task<I, O>> TaskBuilder<I, O, T>.paged() = TaskBuilder.task(PagedTask(build()))
