package com.zetavision.panda.ums.ui.upkeep

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.TextUtils
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.Spinner
import android.widget.TextView
import com.zetavision.panda.ums.R
import com.zetavision.panda.ums.adapter.CommonSpinnerAdapter
import com.zetavision.panda.ums.adapter.UpKeepDetailAdapter
import com.zetavision.panda.ums.base.BaseActivity
import com.zetavision.panda.ums.model.*
import com.zetavision.panda.ums.utils.*
import com.zetavision.panda.ums.utils.network.Client
import com.zetavision.panda.ums.utils.network.RxUtils
import com.zetavision.panda.ums.utils.network.UmsApi
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.functions.BiFunction
import io.reactivex.schedulers.Schedulers
import okhttp3.ResponseBody
import org.json.JSONObject
import org.litepal.crud.DataSupport
import java.util.concurrent.TimeUnit

/**
 * Created by wheroj on 2018/1/31.
 * @describe
 */
class UpKeepDetailActivity: BaseActivity() {

    var recyclerView: RecyclerView? = null
    private var formInfoDetail: FormInfoDetail? = null
    private var weatherList: List<Weather>? = null
    private var shiftList: List<Shift>? = null
    private lateinit var maintFormId: String

    private var temperSpinnerAdapter: CommonSpinnerAdapter? = null
    private var classesSpinnerAdapter: CommonSpinnerAdapter? = null
    private  var upKeepDetailAdapter: UpKeepDetailAdapter? = null

    private var compositeDisposable: CompositeDisposable? = null
    override fun getContentLayoutId(): Int {
        return R.layout.activity_upkeep_detail
    }

    override fun init() {
        header.setLeftImage(R.mipmap.back)
        header.setRightText(getString(R.string.common_savedata), R.color.main_color)

        recyclerView = findViewById(R.id.activityUpKeepDetail_recyclerView)
        recyclerView?.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)

        maintFormId = intent.getStringExtra("maintFormId")
        loadData()
    }

    private fun loadData() {

        if(NetUtils.isNetConnect(`this`)) {
            Observable.zip(Client.getApi(UmsApi::class.java).queryWeather(), Client.getApi(UmsApi::class.java).queryShift()
                    , BiFunction<ResponseBody, ResponseBody, HashMap<String, Result>> { t1, t2 ->
                val map = HashMap<String, Result>()

                var resultObject = JSONObject(t1.string())
                val resultWeather = Result()
                resultWeather.returnCode = resultObject.optString("returnCode")
                resultWeather.returnMessage = resultObject.optString("returnMessage")
                resultWeather.returnData = resultObject.optString("returnData")
                map["weather"] = resultWeather

                resultObject = JSONObject(t2.string())
                val resultShift = Result()
                resultShift.returnCode = resultObject.optString("returnCode")
                resultShift.returnMessage = resultObject.optString("returnMessage")
                resultShift.returnData = resultObject.optString("returnData")
                map["shift"] = resultShift

                return@BiFunction map
            }).subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe { map ->
                        val weather = map["weather"]
                        weatherList = weather?.getList(Weather::class.java)
                        if (weatherList != null) {
                            for (i in weatherList!!.indices) {
                                weatherList!![i].saveOrUpdate("weather='" + weatherList!![i].weather + "'")
                            }
                        }

                        val shift = map["shift"]
                        shiftList = shift?.getList(Shift::class.java)
                        if (shiftList != null) {
                            for (i in shiftList!!.indices) {
                                shiftList!![i].saveOrUpdate("shift='" + shiftList!![i].shift + "'")
                            }
                        }

                        loadLocalData()
                    }
        } else {
            weatherList = DataSupport.findAll(Weather::class.java)
            shiftList = DataSupport.findAll(Shift::class.java)
            loadLocalData()
        }
    }

    @SuppressLint("StringFormatInvalid")
    private fun loadLocalData() {
        formInfoDetail = DataSupport.where("(formId = '$maintFormId')").findFirst(FormInfoDetail::class.java, true)
        if(formInfoDetail == null) {
            RxUtils.acquireString(Client.getApi(UmsApi::class.java).downloadMaintForm(maintFormId)
                    , object : RxUtils.DialogListener() {
                override fun onResult(result: Result) {
                    val formInfoDetails = result.getList(FormInfoDetail::class.java)
                    if (formInfoDetails != null && formInfoDetails.size > 0) {
                        formInfoDetail = formInfoDetails[0]
                        header.setTitle(resources.getString(R.string.maint_fill, formInfoDetail!!.form.formCode))
                        initView(formInfoDetail)
                        initSpinner(formInfoDetail, weatherList, shiftList)
                    } else {
                        ToastUtils.show(R.string.data_exception)
                    }
                }
            })
        } else {
            initView(formInfoDetail)
            initSpinner(formInfoDetail, weatherList, shiftList)
            header.setTitle(resources.getString(R.string.maint_fill, formInfoDetail!!.form.formCode))
        }
    }

    private fun initSpinner(formInfoDetail: FormInfoDetail?, weatherList: List<Weather>?, shiftList: List<Shift>?) {

        if (formInfoDetail != null) {
            val temperSpinner = findViewById<Spinner>(R.id.activityUpKeepDetail_temperSpinner)
            val classesSpinner = findViewById<Spinner>(R.id.activityUpKeepDetail_classesSpinner)

            var weathers: List<String> = arrayListOf()
            if (weatherList != null) {
                weathers = weatherList.indices.map { weatherList[it].description }
            }

            var shifts: List<String> = arrayListOf()
            if (shiftList != null) {
                shifts = shiftList.indices.map { shiftList[it].description }
            }

            if (temperSpinnerAdapter == null) {
                temperSpinnerAdapter = CommonSpinnerAdapter(this)
                temperSpinner.adapter = temperSpinnerAdapter
                temperSpinnerAdapter!!.notifyDataSetChanged(weathers)
                temperSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        formInfoDetail.form.weather = weatherList!![position].weather
                    }
                }
            }

            if (TextUtils.isEmpty(formInfoDetail.form.weather)) {
                temperSpinner.setSelection(0)
            } else {
                val weather = Weather()
                weather.weather = formInfoDetail.form.weather
                val indexOf = weatherList?.indexOf(weather) ?: -1
                if (indexOf in weathers.indices) temperSpinner.setSelection(indexOf)
                else {
                    formInfoDetail.form.weather = weatherList?.get(0)?.description ?: ""
                    temperSpinner.setSelection(0)
                }
            }

            if (classesSpinnerAdapter == null) {
                classesSpinnerAdapter = CommonSpinnerAdapter(this)
                classesSpinner.adapter = classesSpinnerAdapter
                classesSpinnerAdapter!!.notifyDataSetChanged(shifts)
                classesSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onNothingSelected(parent: AdapterView<*>?) {

                    }

                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        formInfoDetail.form.shift = shiftList!![position].shift
                    }
                }
            }
            if (TextUtils.isEmpty(formInfoDetail.form.shift)) {
                classesSpinner.setSelection(0)
            } else {
                val shift = Shift()
                shift.shift = formInfoDetail.form.shift
                val indexOf = shiftList?.indexOf(shift) ?: -1
                if (indexOf in shifts.indices) classesSpinner.setSelection(indexOf)
                else {
                    formInfoDetail.form.shift = shiftList!![0].shift
                    classesSpinner.setSelection(0)
                }
            }
        }
    }

    @SuppressLint("WrongViewCast")
    private fun initView(formInfoDetail: FormInfoDetail?) {

        if (formInfoDetail?.formItemList != null) {
            setAdapter(formInfoDetail)

            findViewById<TextView>(R.id.activityUpKeepDetail_deviceCode).text = formInfoDetail.form.equipmentCode
            findViewById<TextView>(R.id.activityUpKeepDetail_maintPeroid).text = formInfoDetail.form.maintPeriodName
            if (Constant.FORM_STATUS_CLOSED == formInfoDetail.form.status
                    || Constant.FORM_STATUS_COMPLETED == formInfoDetail.form.status
                    || Constant.FORM_STATUS_PLANNED == formInfoDetail.form.status) {
                header.setHiddenRight()
            } else {
                header.setRightText(getString(R.string.common_savedata), R.color.main_color)
            }

            val btnMaintStatus = findViewById<Button>(R.id.activityUpKeepDetail_btnMaintStatus)
            val btnSeeSop = findViewById<Button>(R.id.activityUpKeepDetail_btnSeeSop)
            val tvMaintStatus = findViewById<TextView>(R.id.activityUpKeepDetail_tvMaintStatus)
            val tvMaintStatusStr = findViewById<TextView>(R.id.activityUpKeepDetail_tvMaintStatusStr)

            btnSeeSop.setOnClickListener {
                val where = if (FormInfo.ACTION_TYPE_M == formInfoDetail.actionType) {
                    "flowCode = '" + formInfoDetail.form.maintFlowCode + "'"
                } else {
                    "flowCode = '" + formInfoDetail.form.inspectFlowCode + "'"
                }
                val sopMap = DataSupport.where(where).findFirst(SopMap::class.java)
                if (sopMap != null) {
                    OpenFileUtils.Companion.getInstance().openFile(sopMap.sopLocalPath)
                } else {
                    ToastUtils.show(R.string.no_local_data)
                }
            }

            when (formInfoDetail.form.status) {
//                "表单状态：已结束"
                Constant.FORM_STATUS_CLOSED -> tvMaintStatusStr.text = getString(R.string.formstatus).plus(getString(R.string.formstatus_end))
                Constant.FORM_STATUS_PLANNED -> {
                    tvMaintStatusStr.text = getString(R.string.formstatus).plus(getString(R.string.formstatus_planed))
                    btnMaintStatus.text = resources.getString(R.string.maint_start)
                    val drawable: Drawable = resources.getDrawable(R.mipmap.start)
                    btnMaintStatus.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                Constant.FORM_STATUS_INPROGRESS -> {
                    compositeDisposable?.dispose()
                    compositeDisposable?.clear()
                    compositeDisposable = CompositeDisposable()
                    compositeDisposable?.add(Observable.interval(0, 1, TimeUnit.SECONDS)
                            .observeOn(AndroidSchedulers.mainThread())
                            .subscribeOn(Schedulers.io())
                            .subscribe {
                                val useTime = TimeUtils.getUseTime(formInfoDetail.form.startTime)
                                tvMaintStatusStr.text = getString(R.string.formstatus).plus(getString(R.string.formstatus_ing)).plus(useTime)
                            })

                    btnMaintStatus.text = resources.getString(R.string.maint_finish)
                    val drawable: Drawable = resources.getDrawable(R.mipmap.done)
                    btnMaintStatus.setCompoundDrawablesWithIntrinsicBounds(drawable, null, null, null)
                }
                Constant.FORM_STATUS_COMPLETED -> {
                    compositeDisposable?.dispose()
                    compositeDisposable?.clear()

//                    "表单状态：已完成"
                    tvMaintStatusStr.text = getString(R.string.formstatus).plus(getString(R.string.formstatus_finish))
                    btnMaintStatus.visibility = View.GONE
                    tvMaintStatus.visibility = View.VISIBLE
                }
            }

            btnMaintStatus.setOnClickListener {
                when (formInfoDetail.form.status) {
                    Constant.FORM_STATUS_PLANNED -> {
                        formInfoDetail.isUpload = FormInfo.WAIT
                        formInfoDetail.form.status = Constant.FORM_STATUS_INPROGRESS
                        formInfoDetail.form.startTime = System.currentTimeMillis() / 1000
                        val user = DataSupport.findLast(User::class.java)
                        if (user != null) formInfoDetail.form.startUser = user.USERNAME
                        formInfoDetail.form.saveOrUpdate("formId='${formInfoDetail.form.formId}'")
                        formInfoDetail.saveOrUpdate("formId='${formInfoDetail.formId}'")
                        initView(formInfoDetail)
                    }
                    Constant.FORM_STATUS_INPROGRESS -> {
                        val emptyIndex = ArrayList<Int>()
                        val checkDataSize = checkData(formInfoDetail, true, emptyIndex)
                        if (checkDataSize == formInfoDetail.formItemList.size) {
                            if (emptyIndex.isNotEmpty()) {
                                val buffer = StringBuffer()
                                buffer.append(getString(R.string.order).plus("："))
                                for (i in emptyIndex.indices) {
                                    if (i == emptyIndex.size - 1) {
                                        buffer.append(emptyIndex[i] + 1)
                                    } else {
                                        buffer.append((emptyIndex[i] + 1).toString().plus("、"))
                                    }
                                }
                                buffer.append("的保养数据未录入!")
                                showNotice(buffer.toString())
                            } else {
                                changeStatusAndSave(formInfoDetail)
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * 保存所有的item的hash值
     */
    private var hashCodeMap:HashMap<String, Int> = HashMap()
    private fun setAdapter(formInfoDetail: FormInfoDetail) {
        if (upKeepDetailAdapter == null) {
            hashCodeMap.clear()
            hashCodeMap[formInfoDetail.form.formId] = formInfoDetail.form.hashCode()
            formInfoDetail.formItemList.forEach {
                hashCodeMap[it.formItemId] = it.hashCode()
            }

            upKeepDetailAdapter = UpKeepDetailAdapter(formInfoDetail)
            recyclerView?.adapter = upKeepDetailAdapter
        } else {
            upKeepDetailAdapter!!.updateData(formInfoDetail.form.status)
        }
    }

    private fun changeStatusAndSave(formInfoDetail: FormInfoDetail) {
        formInfoDetail.form.status = Constant.FORM_STATUS_COMPLETED
        formInfoDetail.form.completeTime = System.currentTimeMillis() / 1000
        val user = DataSupport.findLast(User::class.java)
        if (user != null) formInfoDetail.form.completeUser = user.USERNAME

        if (saveData(formInfoDetail)) {
            ToastUtils.show(R.string.save_success)
        } else {
            ToastUtils.show(R.string.save_fail)
        }
        initView(formInfoDetail)
    }

    private fun showNotice(notice: String) {
        val builder = AlertDialog.Builder(`this`)
        builder.setMessage(notice)
        builder.setNeutralButton(R.string.cancel) { dialog, which ->
            builder.create().dismiss()
        }

        builder.setPositiveButton(R.string.keep_save) { dialog, which ->
            changeStatusAndSave(formInfoDetail!!)
        }
        builder.create().show()
    }

    override fun getHasTitle(): Boolean {
        return true
    }

    override fun onLeftClick() {
        if (formInfoDetail?.form?.status == Constant.FORM_STATUS_INPROGRESS) {
            showDialog()
        } else {
            finish()
        }
    }

    private fun showDialog() {
        val builder = AlertDialog.Builder(`this`)
        builder.setMessage(R.string.notice_save_data)
        builder.setNeutralButton(R.string.keep_save) { dialog, which ->
            builder.create().dismiss()
        }

        builder.setPositiveButton(R.string.sure_save) { dialog, which ->
            `this`.finish()
        }
        builder.create().show()
    }

    override fun onBackPressed() {
        if (formInfoDetail?.form?.status == Constant.FORM_STATUS_INPROGRESS) {
            showDialog()
        } else {
            super.onBackPressed()
        }
    }

    override fun onRightTextClick() {
        checkAndSaveData(false)
    }

    /**
     * 是否进行拍照检测
     */
    private fun checkAndSaveData(checkPhoto: Boolean, emptyIndex: ArrayList<Int>? = null):Boolean {
        if (checkData(formInfoDetail, checkPhoto, emptyIndex) == formInfoDetail?.formItemList?.size) {
            return if (saveData(formInfoDetail)) {
                ToastUtils.show(R.string.save_success)
                true
            } else {
                ToastUtils.show(R.string.save_fail)
                false
            }
        }
        return false
    }

    /**
     * @return 检测的数据量
     */
    private fun checkData(formInfoDetail: FormInfoDetail?, checkDetail: Boolean, emptyIndex: ArrayList<Int>?): Int {
        if (formInfoDetail != null) {
            for (i in formInfoDetail.formItemList.indices) {
                val formItem = formInfoDetail.formItemList[i]
                if (TextUtils.isEmpty(formItem.result)) {
                    if (checkDetail && emptyIndex != null) emptyIndex.add(i)
                } else {
                    if ("N" == formItem.valueType) {
                        try {
                            formItem.result.toFloat()
                        } catch (e: NumberFormatException) {
                            e.printStackTrace()
                            ToastUtils.show("序号" + (i + 1) + "的取值必须是数字")
                            return i
                        } catch (e: NullPointerException) {
                            e.printStackTrace()
                            ToastUtils.show("序号" + (i + 1) + "的取值不能为空")
                            return i
                        }
                    }
                }

                if (!TextUtils.isEmpty(formItem.remarks)) {
                    if (formItem.remarks.length > Constant.MAX_LEN) {
                        ToastUtils.show("序号" + (i + 1) + "的备注超过200的长度限制")
                        return i
                    }
                }
            }

            if (!TextUtils.isEmpty(formInfoDetail.form.fillinRemarks)) {
                if (formInfoDetail.form.fillinRemarks.length > Constant.MAX_LEN) {
                    ToastUtils.show("备注超过200的长度限制")
                    return -1
                }
            }
            return formInfoDetail.formItemList.size
        }
        return -1
    }

    private fun saveData(formInfoDetail: FormInfoDetail?): Boolean {
        if (formInfoDetail != null) {
            var isSuccessSave: Boolean
            formInfoDetail.formItemList.forEach {
                if (hashCodeMap[it.formItemId] != it.hashCode()) {
                    isSuccessSave = it.saveOrUpdate("(formItemId='${it.formItemId}')")
                    if (!isSuccessSave) return isSuccessSave
                    hashCodeMap[it.formItemId] = it.hashCode()
                }
            }

            if (formInfoDetail.form.hashCode() != hashCodeMap[formInfoDetail.formId]) {
                val formInfo = formInfoDetail.form
                isSuccessSave = formInfo.saveOrUpdate("(formId='${formInfo.formId}')")
                if (!isSuccessSave) return isSuccessSave
                hashCodeMap[formInfoDetail.formId] = formInfo.hashCode()
            }

            formInfoDetail.isUpload = 1
            return formInfoDetail.saveOrUpdate("(formId='${formInfoDetail.formId}')")
        }
        return false
    }

    override fun onPause() {
        super.onPause()
        compositeDisposable?.dispose()
    }
}