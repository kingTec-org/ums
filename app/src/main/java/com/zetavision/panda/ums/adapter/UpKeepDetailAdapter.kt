package com.zetavision.panda.ums.adapter

import android.text.Editable
import android.text.TextUtils
import android.view.View
import android.widget.*
import com.chad.library.adapter.base.BaseQuickAdapter
import com.chad.library.adapter.base.BaseViewHolder
import com.zetavision.panda.ums.R
import com.zetavision.panda.ums.base.TextWatcherImpl
import com.zetavision.panda.ums.model.FormItem
import com.zetavision.panda.ums.utils.Constant

/**
 * Created by wheroj on 2018/1/31 17:08.
 * @describe
 */
class UpKeepDetailAdapter(data: List<FormItem>, var status: String): BaseQuickAdapter<FormItem, BaseViewHolder>(R.layout.item_upkeep_param, data) {
    private var watcher: TextWatcherImpl? = null

    override fun convert(helper: BaseViewHolder?, item: FormItem?) {
        if (item != null) {
            helper?.setText(R.id.itemUpKeepParam_order, (data.indexOf(item) + 1).toString())
                    ?.setText(R.id.itemUpKeepParam_code, item.basicActionName)
                    ?.setText(R.id.itemUpKeepParam_unit, item.unit)

            val tvValue = helper?.getView<TextView>(R.id.itemUpKeepParam_tvValue)
            val tvRemark = helper?.getView<TextView>(R.id.itemUpKeepParam_tvRemark)
            val rlInput = helper?.getView<RelativeLayout>(R.id.itemUpKeepParam_rlInput)
            val etInput = helper?.getView<EditText>(R.id.itemUpKeepParam_etInput)
            val tvInput = helper?.getView<TextView>(R.id.itemUpKeepParam_tvInput)
            val inputNotice = helper?.getView<TextView>(R.id.itemUpKeepParam_inputNotice)
            val inputNoticeImg = helper?.getView<ImageView>(R.id.itemUpKeepParam_inputNoticeImg)
            val rlChoose = helper?.getView<RelativeLayout>(R.id.itemUpKeepParam_choose)
            val spinner = helper?.getView<Spinner>(R.id.itemUpKeepParam_spinner)
            val etRemark = helper?.getView<EditText>(R.id.itemUpKeepParam_remark)

            watcher = object : TextWatcherImpl() {

                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                    if ("T" == item.valueType) {
                        item.result = s.toString()
                    } else {
                        checkFloatValue(s, item, inputNotice, inputNoticeImg, etInput!!, tvInput!!)
                    }
                }
            }

            when (status) {
                Constant.FORM_STATUS_COMPLETED,
                Constant.FORM_STATUS_CLOSED,
                Constant.FORM_STATUS_PLANNED -> {
                    when (item.valueType) {
                        "N" -> {
                            rlInput?.visibility = View.VISIBLE
                            etInput?.visibility = View.GONE
                            tvInput?.visibility = View.VISIBLE
                            tvValue?.visibility = View.GONE
                        }
                        else -> {
                            rlInput?.visibility = View.GONE
                            tvValue?.visibility = View.VISIBLE
                        }
                    }
                    rlChoose?.visibility = View.GONE
                    etRemark?.visibility = View.GONE
                    tvRemark?.visibility = View.VISIBLE

                    if ("B" == item.valueType) {
                        val list = item.optionValues.split("|")
                        val valueList = item.optionValuesDescription.split("|")

                        var indexOf = if (TextUtils.isEmpty(item.result)) {
                            list.indexOf(item.presetValue)
                        } else {
                            list.indexOf(item.result)
                        }
                        if (indexOf in valueList.indices) {
                            tvValue?.text = valueList[indexOf]
                        } else {
                            tvValue?.text = ""
                        }
                    } else if ("N" == item.valueType){
                        tvInput?.text = item.result ?: ""
                    } else {
                        tvValue?.text = if (TextUtils.isEmpty(item.result)) item.presetValue?:"" else item.result
                    }
                    tvRemark?.text = item.remarks
                }
                Constant.FORM_STATUS_INPROGRESS -> {
                    when (item.valueType) {
                        "N", "T" -> {
                            rlInput?.visibility = View.VISIBLE
                            etInput?.visibility = View.VISIBLE
                            tvInput?.visibility = View.GONE
                            rlChoose?.visibility = View.GONE
                        }
                        "O", "B" -> {
                            rlInput?.visibility = View.GONE
                            rlChoose?.visibility = View.VISIBLE
                        }
                    }
                    tvValue?.visibility = View.GONE
                    tvRemark?.visibility = View.GONE
                    etRemark?.visibility = View.VISIBLE
                }
            }

            etRemark?.setText(item.remarks)
            etRemark?.addTextChangedListener(object : TextWatcherImpl() {

                override fun afterTextChanged(s: Editable?) {
                    item.remarks = s.toString()
                }
            })

            when (item.valueType) {
                "N", "T" -> {
                    helper?.setIsRecyclable(false)
                    helper?.setText(R.id.itemUpKeepParam_lowerLimit, item.lowerLimit)
                            ?.setText(R.id.itemUpKeepParam_upperLimit, item.upperLimit)

                    etInput?.removeTextChangedListener(watcher)
                    when (item.valueType) {
                        "N" -> {
                            etInput?.setText(item.result ?: "")
                            checkFloatValue(item.result, item, inputNotice, inputNoticeImg, etInput!!, tvInput!!)
                        }
                        "T" -> {
                            inputNotice?.visibility = View.GONE
                            inputNoticeImg?.visibility = View.GONE
                            etInput?.setText(if (TextUtils.isEmpty(item.result)) item.presetValue?:"" else item.result)
                        }
                    }
                    etInput?.addTextChangedListener(watcher)
                }
                "O" -> {
                    val list = item.optionValues.split("|")
                    val spinnerAdapter = CommonSpinnerAdapter(mContext)
                    spinner?.adapter = spinnerAdapter
                    spinnerAdapter.notifyDataSetChanged(list)

                    val indexOf = if (TextUtils.isEmpty(item.result)) {
                        if (!TextUtils.isEmpty(item.presetValue)) {
                            list.indexOf(item.presetValue)
                        } else -1
                    } else list.indexOf(item.result)

                    if (indexOf in list.indices)
                        spinner?.setSelection(indexOf)
                    else {
                        if (list.isNotEmpty())
                            spinner?.setSelection(0)
                    }

                    spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                            item.result = list[position]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {

                        }
                    }
                }
                "B" -> {
                    val valueList = item.optionValues.split("|")
                    val list = item.optionValuesDescription.split("|")
                    val spinnerAdapter = CommonSpinnerAdapter(mContext)
                    spinner?.adapter = spinnerAdapter
                    spinnerAdapter.notifyDataSetChanged(list)

                    val indexOf = if (TextUtils.isEmpty(item.result)) {
                        if (!TextUtils.isEmpty(item.presetValue)) {
                            valueList.indexOf(item.presetValue)
                        } else -1
                    } else valueList.indexOf(item.result)

                    if (indexOf in list.indices)
                        spinner?.setSelection(indexOf)
                    else {
                        if (list.isNotEmpty())
                            spinner?.setSelection(0)
                    }

                    spinner?.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                        override fun onItemSelected(parent: AdapterView<*>, view: View, position: Int, id: Long) {
                            item.result = valueList[position]
                        }

                        override fun onNothingSelected(parent: AdapterView<*>) {

                        }
                    }
                }
            }
        }
    }

    private fun checkFloatValue(content: CharSequence?, item: FormItem, inputNotice: TextView?, inputNoticeImg: ImageView?, etInput: EditText, tvInput: TextView) {
        try {
            if (!TextUtils.isEmpty(content)) {
                val intValue = content.toString().toFloat()
                var lowerLimit = Float.MIN_VALUE
                var upperLimit = Float.MAX_VALUE
                if (!TextUtils.isEmpty(item.lowerLimit)) {
                    lowerLimit = item.lowerLimit.toFloat()
                }

                if (!TextUtils.isEmpty(item.upperLimit)) {
                    upperLimit = item.upperLimit.toFloat()
                }
                when {
                    intValue < lowerLimit -> {
                        inputNotice?.visibility = View.VISIBLE
                        inputNoticeImg?.visibility = View.VISIBLE
                        inputNotice?.text = mContext.resources.getString(R.string.too_lower)
                        etInput.setBackgroundResource(R.drawable.radius_red_5)
                        tvInput.setBackgroundResource(R.drawable.radius_red_5)
                        item.result = content.toString()
                    }
                    intValue > upperLimit -> {
                        inputNotice?.visibility = View.VISIBLE
                        inputNoticeImg?.visibility = View.VISIBLE
                        inputNotice?.text = mContext.resources.getString(R.string.too_upper)
                        etInput.setBackgroundResource(R.drawable.radius_red_5)
                        tvInput.setBackgroundResource(R.drawable.radius_red_5)
                        item.result = content.toString()
                    }
                    else -> {
                        inputNotice?.visibility = View.GONE
                        inputNoticeImg?.visibility = View.INVISIBLE
                        etInput.setBackgroundResource(R.drawable.radius_blue_5)
                        tvInput.setBackgroundResource(R.drawable.radius_blue_5)
                        item.result = content.toString()
                    }
                }
            } else {
                inputNotice?.visibility = View.GONE
                inputNoticeImg?.visibility = View.INVISIBLE
                etInput.setBackgroundResource(R.drawable.radius_blue_5)
                tvInput.setBackgroundResource(R.drawable.radius_blue_5)
                item.result = content.toString()
            }
        } catch (e: NumberFormatException) {
            e.printStackTrace()
            item.result = content.toString()
        } catch (e: NullPointerException) {
            item.result = ""
            e.printStackTrace()
        }
    }

    fun updateData(status: String) {
        this.status = status
        notifyDataSetChanged()
    }
}