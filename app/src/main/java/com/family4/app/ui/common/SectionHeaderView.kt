package com.family4.app.ui.common

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.core.content.res.use
import com.family4.app.R

/**
 * The standard branded header bar used at the top of every screen.
 *
 * Replaces the copy-pasted logo + title block: one component means one place
 * to change the app's header treatment.
 *
 * ```xml
 * <com.family4.app.ui.common.SectionHeaderView
 *     android:layout_width="match_parent"
 *     android:layout_height="wrap_content"
 *     app:headerTitle="Health"
 *     app:headerActionIcon="@drawable/ic_more_vert"/>
 * ```
 */
class SectionHeaderView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayout(context, attrs, defStyleAttr) {

    private val logoView: ImageView
    private val titleView: TextView
    private val actionView: ImageView

    /** Invoked when the trailing action icon is tapped. */
    var onActionClick: (() -> Unit)? = null
        set(value) {
            field = value
            actionView.setOnClickListener { value?.invoke() }
        }

    init {
        orientation = HORIZONTAL
        gravity = android.view.Gravity.CENTER_VERTICAL
        minimumHeight = resources.getDimensionPixelSize(R.dimen.section_header_height)
        setBackgroundColor(ContextCompat.getColor(context, R.color.bg_surface))
        val padH = resources.getDimensionPixelSize(R.dimen.section_header_padding)
        setPadding(padH, 0, padH, 0)

        LayoutInflater.from(context).inflate(R.layout.view_section_header, this, true)
        logoView = findViewById(R.id.headerLogo)
        titleView = findViewById(R.id.headerTitle)
        actionView = findViewById(R.id.headerAction)

        context.obtainStyledAttributes(attrs, R.styleable.SectionHeaderView).use { a ->
            titleView.text = a.getString(R.styleable.SectionHeaderView_headerTitle).orEmpty()

            logoView.visibility =
                if (a.getBoolean(R.styleable.SectionHeaderView_headerShowLogo, true)) VISIBLE else GONE

            val actionRes = a.getResourceId(R.styleable.SectionHeaderView_headerActionIcon, 0)
            if (actionRes != 0) {
                actionView.setImageResource(actionRes)
                actionView.visibility = VISIBLE
                actionView.contentDescription =
                    a.getString(R.styleable.SectionHeaderView_headerActionDescription)
            }
        }
    }

    /** Updates the title at runtime (e.g. a conversation partner's name). */
    fun setTitle(text: CharSequence) {
        titleView.text = text
    }

    /** Shows or hides the trailing action icon. */
    fun setActionVisible(visible: Boolean) {
        actionView.visibility = if (visible) VISIBLE else GONE
    }
}
