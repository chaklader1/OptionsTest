package com.activfinancial.samples.contentgatewayapi.optionseriessample;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.activfinancial.contentplatform.contentgatewayapi.ContentGatewayClient;
import com.activfinancial.contentplatform.contentgatewayapi.ContentGatewayClient.ConnectParameters;
import com.activfinancial.contentplatform.contentgatewayapi.FieldListValidator;
import com.activfinancial.contentplatform.contentgatewayapi.common.RequestBlock;
import com.activfinancial.contentplatform.contentgatewayapi.consts.Exchange;
import com.activfinancial.contentplatform.contentgatewayapi.consts.FieldIds;
import com.activfinancial.middleware.StatusCode;
import com.activfinancial.middleware.activbase.MiddlewareException;
import com.activfinancial.middleware.application.Application;
import com.activfinancial.middleware.application.Settings;
import com.activfinancial.middleware.fieldtypes.Date;
import com.activfinancial.middleware.fieldtypes.Rational;
import com.activfinancial.middleware.service.FileConfiguration;
import com.activfinancial.middleware.service.ServiceApi;
import com.activfinancial.middleware.service.ServiceInstance;
import com.activfinancial.samples.common.ui.io.UiIo;
import com.activfinancial.samples.common.ui.io.UiIo.LogType;


public class OptionSeries {

    private UiIo uiIo = new UiIo();

    // The service id.
    private String serviceId;

    // The serviced instance id
    private String serviceInstanceId;

    // The user id.
    private String userId;

    // The password.
    private String password;

    // CG instance
    ContentGatewayClient client;

    // main entry point into application
    public static void main(String[] args) {
        new OptionSeries().run();
    }

    // application entry point
    private void run() {
        this.serviceId = System.getProperty("I", "Service.ContentGateway");
        this.serviceInstanceId = System.getProperty("N", null);

        this.userId = "drwt1000-dwmduat";
        this.password = "dwmduat";

        Settings settings = new Settings();

        Application application = new Application(settings);

        application.startThread();

        this.client = new ContentGatewayClient(application);

        if (!connect())
            return;

        runExample();

        application.postDiesToThreads();
        application.waitForThreadsToExit();
    }

    private void runExample() {
        // set up filter
        OptionSeriesFilter optionSeriesFilter = new OptionSeriesFilter();

        String symbol = "TWTR";

        setupFilter(optionSeriesFilter);

        // Construct this request block just once and cache it.
        RequestBlock requestBlockOptions = new RequestBlock();

        requestBlockOptions.fieldIdList.add(FieldIds.FID_SYMBOL);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_EXPIRATION_DATE);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_STRIKE_PRICE);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_OPTION_TYPE);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_TRADE);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_BID);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_ASK);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_CUMULATIVE_VOLUME);
        requestBlockOptions.fieldIdList.add(FieldIds.FID_OPEN_INTEREST);
        // add more fields as needed.

        // FLV could be fetched from the thread local storage instead of constructing them each call.
        // Will be using one fieldListValidator instance to minimize object construction.
        FieldListValidator fieldListValidator = new FieldListValidator(this.client);
        List<OptionInfo> options = new ArrayList<OptionInfo>();

        // get all options for an underling
        StatusCode statusCode = GetOptionSeriesHelper.getOptionSeries(this.client, fieldListValidator, symbol, optionSeriesFilter, requestBlockOptions, options);

        if (statusCode == StatusCode.STATUS_CODE_SUCCESS) {
            for (OptionInfo optionInfo : options) {
                // dump to the screen
                uiIo.logMessage(LogType.LOG_TYPE_INFO, optionInfo.toString());
            }
        }

        // now disconnect
        this.client.disconnect();
    }

    private void setupFilter(OptionSeriesFilter optionSeriesFilter) {

//        optionSeriesFilter.setStartDate(null);
//        optionSeriesFilter.setEndDate(null);
//        optionSeriesFilter.setLowStrike(null);
//        optionSeriesFilter.setHighStrike(null);
//
//        // both calls and puts
//        optionSeriesFilter.setCallPut(OptionSeriesFilter.CallPutEnum.BOTH);
//
//        optionSeriesFilter.setAtTheMoney(false);

        List<String> exchangeList = new ArrayList<String>();

        exchangeList.add(Exchange.EXCHANGE_US_OPTIONS_COMPOSITE);
        optionSeriesFilter.setExchangeList(exchangeList);
    }

    private boolean connect() {
        StatusCode statusCode;

        // first stage to connect is to find a service to connect to
        List<ServiceInstance> serviceInstanceList = new ArrayList<ServiceInstance>();

        Map<String, Object> attributes = new HashMap<String, Object>();
        attributes.put(FileConfiguration.FILE_LOCATION, this.client.getApplication().getSettings().serviceLocationIniFile);
        statusCode = ServiceApi.findServices(ServiceApi.CONFIGURATION_TYPE_FILE, this.serviceId, attributes, serviceInstanceList);

        if (StatusCode.STATUS_CODE_SUCCESS != statusCode) {
            uiIo.logMessage(LogType.LOG_TYPE_ERROR, "FindServices() failed, error - " + statusCode.toString());
            return false;
        }

        // here we are just going to pick the first service that is returned,
        // and its first access point url
        ConnectParameters connectParameters = new ConnectParameters();
        ServiceInstance serviceInstance = serviceInstanceList.get(0);

        if (serviceInstanceId != null) {
            for (ServiceInstance si : serviceInstanceList) {
                if (si.serviceAccessPointList.get(0).id.equals(serviceInstanceId)) {
                    serviceInstance = si;
                    break;
                }
            }
        }

        connectParameters.serviceId = serviceInstance.serviceId;
        connectParameters.url = serviceInstance.serviceAccessPointList.get(0).url;
        connectParameters.userId = userId;
        connectParameters.password = password;

        statusCode = this.client.connect(connectParameters, ContentGatewayClient.DEFAULT_TIMEOUT);

        if (StatusCode.STATUS_CODE_SUCCESS != statusCode)
            uiIo.logMessage(LogType.LOG_TYPE_ERROR, "Connect() failed, error - " + statusCode.toString());

        return statusCode == StatusCode.STATUS_CODE_SUCCESS;
    }
}
