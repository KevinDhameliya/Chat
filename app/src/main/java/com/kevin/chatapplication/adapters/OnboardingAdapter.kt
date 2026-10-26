package com.kevin.chatapplication.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.recyclerview.widget.RecyclerView
import com.kevin.chatapplication.databinding.ItemOnboardingPageBinding

data class OnboardingPage(
    @StringRes val title: Int,
    @StringRes val description: Int,
    @DrawableRes val mainIcon: Int,
    @DrawableRes val accentTopIcon: Int,
    @DrawableRes val accentBottomIcon: Int
)

class OnboardingAdapter(
    private val pages: List<OnboardingPage>
) : RecyclerView.Adapter<OnboardingAdapter.PageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PageViewHolder =
        PageViewHolder(
            ItemOnboardingPageBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        )

    override fun onBindViewHolder(holder: PageViewHolder, position: Int) =
        holder.bind(pages[position])

    override fun getItemCount(): Int = pages.size

    class PageViewHolder(private val binding: ItemOnboardingPageBinding) :
        RecyclerView.ViewHolder(binding.root) {

        fun bind(page: OnboardingPage) = with(binding) {
            tvTitle.setText(page.title)
            tvDescription.setText(page.description)
            ivMain.setImageResource(page.mainIcon)
            ivAccentTop.setImageResource(page.accentTopIcon)
            ivAccentBottom.setImageResource(page.accentBottomIcon)
        }
    }
}
