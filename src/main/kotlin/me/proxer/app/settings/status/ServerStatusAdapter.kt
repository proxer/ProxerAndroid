package me.proxer.app.settings.status

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mikepenz.iconics.IconicsDrawable
import com.mikepenz.iconics.typeface.IIcon
import com.mikepenz.iconics.typeface.library.community.material.CommunityMaterial
import com.mikepenz.iconics.utils.toIconicsColorRes
import com.mikepenz.iconics.utils.toIconicsSizeDp
import kotterknife.bindView
import me.proxer.app.R
import me.proxer.app.base.BaseAdapter
import me.proxer.app.settings.status.ServerStatusAdapter.ViewHolder
import me.proxer.app.util.extension.setIconicsImage

/**
 * @author Ruben Gees
 */
class ServerStatusAdapter : BaseAdapter<ServerStatus, ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(LayoutInflater.from(parent.context).inflate(R.layout.item_server_status, parent, false))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(data[position])
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        internal val icon: ImageView by bindView(R.id.icon)
        internal val name: TextView by bindView(R.id.name)
        internal val status: ImageView by bindView(R.id.status)

        fun bind(item: ServerStatus) {
            val typeIcon: IIcon = when (item.type) {
                ServerType.MAIN -> CommunityMaterial.Icon2.cmd_server_network
                ServerType.MANGA -> CommunityMaterial.Icon.cmd_book_open_page_variant
                ServerType.STREAM -> CommunityMaterial.Icon2.cmd_television
            }

            val statusIcon = when (item.online) {
                true -> CommunityMaterial.Icon.cmd_earth
                false -> CommunityMaterial.Icon.cmd_earth_off
            }

            icon.setIconicsImage(typeIcon, sizeDp = 32)

            name.text = item.name

            status.setImageDrawable(
                IconicsDrawable(status.context, statusIcon)
                    .size(32.toIconicsSizeDp())
                    .padding(8.toIconicsSizeDp())
                    .color((if (item.online) R.color.md_green_500 else R.color.md_red_500).toIconicsColorRes())
            )
        }
    }
}
