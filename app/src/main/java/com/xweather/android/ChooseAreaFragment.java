package com.xweather.android;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xweather.android.db.City;
import com.xweather.android.db.County;
import com.xweather.android.db.Province;
import com.xweather.android.util.HttpUtil;
import com.xweather.android.util.Utility;

import org.litepal.LitePal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.Response;

public class ChooseAreaFragment extends Fragment {
    public static final int LEVEL_PROVINCE = 0;

    public static final int LEVEL_CITY = 1;

    public static final int LEVEL_COUNTY = 2;

    private ProgressDialog progressDialog;

    private TextView titleText;

    private Button backButton;

    private ListView listView;

    private ArrayAdapter<String> adapter;

    private List<String> dataList = new ArrayList<>();

    /**
     * 省列表
     */
    private List<Province> provinceList;

    /**
     * 市列表
     */
    private List<City> cityList;

/** * 县列表
 */
private List<County> countyList;

    /**
     * 选中的省份
     */
    private Province selectedProvince;

    /**
     * 选中的城市
     */
    private City selectedCity;

    /**
     * 当前选中的级别
     */
    private int currentLevel;


    /**
     * 获取到了一些控件的实例，然后去初始化了ArrayAdapter，
     * 并将它设置为ListView的适配器。
     * */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.choose_area, container, false);
        titleText = view.findViewById(R.id.title_text);
        backButton = view.findViewById(R.id.back_button);
        listView = view.findViewById(R.id.list_view);
        adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_list_item_1, dataList);
        listView.setAdapter(adapter);
        return view;
    }
    /**
      * 接着在onActivityCreated() 方法中给ListView和Button设置了点击事件，
      * 到这里我们的初始化工作就算是完成了。
      * */
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState); listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                if (currentLevel == LEVEL_PROVINCE) {
                    selectedProvince = provinceList.get(position);
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    selectedCity = cityList.get(position);
                    queryCounties();
                }
            }
        });
        backButton.setOnClickListener(new View.OnClickListener() {//返回按钮的点击事件
            /**
             * 对当前ListView的列表级别进行判断。如果当前是县级列表，那么就返回到市级列表，
             * 如果当前是市级列表，那么就返回到省级表列表。当返回到省级列表时，
             * 返回按钮会自动隐藏，从而也就不需要再做进一步的处理了。
             * */
            @Override
            public void onClick(View v) {
                if (currentLevel == LEVEL_COUNTY) {
                    queryCities();
                } else if (currentLevel == LEVEL_CITY) {
                    queryProvinces();
                }
            }
        });
        /**
         从这里开始加载省级数据的
         */
        queryProvinces();
    }

    /**
     * 查询全国所有的省，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryProvinces() {
        /**将头布局的标题设置成中国*/
        titleText.setText("中国");
        /**将返回按钮隐藏起来，因为省级列表已经不能再返回了*/
        backButton.setVisibility(View.GONE);
        /**然后调用LitePal的查询接口来从数据库中读取省级数据*/
        provinceList = LitePal.findAll(Province.class);
        if (provinceList.size() > 0) {
            /**如果读取到了就直接将数据显示到界面上*/
            dataList.clear();
            for (Province province : provinceList) {
                dataList.add(province.getProvinceName());}
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_PROVINCE;
        } else {
            /**如果没有读取到就按如下接口组装出一个请求地址*/
            String address = "http://guolin.tech/api/china";
            /**然后调用queryFromServer() 方法来从服务器上查询数据*/
            queryFromServer(address, "province");
        }
    }

    /**
     * 查询选中省内所有的市，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCities() {
        titleText.setText(selectedProvince.getProvinceName());
        backButton.setVisibility(View.VISIBLE);
        cityList = LitePal.where("provinceid = ?", String.valueOf(selectedProvince.getId())).find(City.class);
        if (cityList.size() > 0) {
            dataList.clear();
            for (City city : cityList) {
                dataList.add(city.getCityName());
            }
            adapter.notifyDataSetChanged();
            listView.setSelection(0);
            currentLevel = LEVEL_CITY;
        } else {
            int provinceCode = selectedProvince.getProvinceCode();
            String address = "http://guolin.tech/api/china/" + provinceCode;
            queryFromServer(address, "city");
        }
    }

    /**
     * 查询选中市内所有的县，优先从数据库查询，如果没有查询到再去服务器上查询
     */
    private void queryCounties(){
        titleText.setText(selectedCity.getCityName());
        backButton.setVisibility(View.VISIBLE);
        countyList = LitePal.where("cityid = ?", String.valueOf(selectedCity.getId())).find(County.class);
        if (countyList.size() > 0) {
        dataList.clear();
        for (County county : countyList) {
            dataList.add(county.getCountyName());
        }
        adapter.notifyDataSetChanged();
        listView.setSelection(0);
        currentLevel = LEVEL_COUNTY;
    } else {
        int provinceCode = selectedProvince.getProvinceCode();
        int cityCode = selectedCity.getCityCode();
        String address = "http://guolin.tech/api/china/" + provinceCode + "/" + cityCode;
        queryFromServer(address, "county");
    }
}

    /**
     * 根据传入的地址和类型从服务器上查询省市县数据
     */
    private void queryFromServer(String address, final String type) {
        /**
         * queryFromServer() 方法中会调用HttpUtil的sendOkHttpRequest() 方法来
         * 向服务器发送请求，响应的数据会回调到onResponse() 方法中，然后我们在这里去
         * 调用Utility的handleProvincesResponse() 方法来解析和
         * 处理服务器返回的数据，并存储到数据库中。
         * */
        showProgressDialog();
        HttpUtil.sendOkHttpRequest(address, new Callback() {
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                String responseText = response.body().string();//接收sendOkHttpRequest回调的数据
                boolean result = false;
                /**调用Utility的handleProvincesResponse() 方法来
                 * 解析和处理服务器返回的数据，并存储到数据库中。
                 * */
                if ("province".equals(type)) {
                    result = Utility.handleProvinceResponse(responseText);
                } else if ("city".equals(type)) {
                    result = Utility.handleCityResponse(responseText,selectedProvince.getId());
                } else if ("county".equals(type)) {
                    result = Utility.handleCountyResponse(responseText,selectedCity.getId());
                }
                if (result) {
                    /**
                     * 在解析和处理完数据之后，我们再次调用了queryProvinces()方法
                     * 来重新加载省级数据，
                     * 由于queryProvinces() 方法牵扯到了UI操作，
                     * 因此必须要在主线程中调用，这里借助了runOnUiThread()方法
                     * 来实现从子线程切换到主线程。
                     * */
                    getActivity().runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            closeProgressDialog();
                            if ("province".equals(type)) {
                                //现在数据库中已经存在了数据，因此调用queryProvinces()
                                // 就会直接将数据显示到界面上了
                                queryProvinces();
                            } else if ("city".equals(type)) {
                                queryCities();
                            } else if ("county".equals(type)) {
                                queryCounties();
                            }
                        }
                    });
                }
            }

            @Override
            public void onFailure(Call call, IOException e) {
                // 通过runOnUiThread()方法回到主线程处理逻辑
                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        closeProgressDialog();
                        Toast.makeText(getContext(), "加载失败", Toast.LENGTH_SHORT).
                                show();
                    }
                });
            }
        });
    }

    /**
     * 显示进度对话框
     */
    private void showProgressDialog() {
        if (progressDialog == null) {
            progressDialog = new ProgressDialog(getActivity());
            progressDialog.setMessage("正在加载...");
            progressDialog.setCanceledOnTouchOutside(false);
        }
        progressDialog.show();
    }

    /**
     * 关闭进度对话框
     */
    private void closeProgressDialog() {
        if (progressDialog != null) {
            progressDialog.dismiss();
        }
    }

}



