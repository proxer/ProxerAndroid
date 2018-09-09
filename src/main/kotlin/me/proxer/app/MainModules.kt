package me.proxer.app

import com.gojuno.koptional.Optional
import me.proxer.app.media.MediaInfoViewModel
import me.proxer.app.media.list.MediaListViewModel
import me.proxer.app.news.NewsViewModel
import me.proxer.app.ucp.media.UcpMediaListViewModel
import me.proxer.library.enums.Category
import me.proxer.library.enums.UserMediaListFilterType
import org.koin.androidx.viewmodel.ext.koin.viewModel
import org.koin.dsl.module.module

val viewModelModule = module {
    viewModel { NewsViewModel() }

    viewModel { parameterList ->
        MediaListViewModel(
            parameterList.get(0),
            parameterList.get(1),
            parameterList.get(2),
            parameterList.get(3),
            parameterList.get(4),
            parameterList.get(5),
            parameterList.get(6),
            parameterList.get(7),
            parameterList.get(8),
            parameterList.get(9),
            parameterList.get(10)
        )
    }

    viewModel { (entryId: String) -> MediaInfoViewModel(entryId) }

    viewModel { (category: Category, filter: Optional<UserMediaListFilterType>) ->
        UcpMediaListViewModel(category, filter)
    }
}
