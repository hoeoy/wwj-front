package com.iandtop.front.smartpark.pos.vo;


public class HandleMealMoneyVO {

    public static final int Meal_Type_Normal = 0;
    public static final int Meal_Type_Count = 1;
    public static final int Meal_Type_Accounting = 5;

    private String card_ineffectived_date;
    private String pk_card;
    private String pk_corp;//刷卡人所在的公司
    private String pk_psnbasdoc;
    private String pk_device;
    private String pk_meal_rule;
    private String card_state;
    private String psncode;
    private String psnname;
    private String device_code;
    private int meal_type;
    private String meal_kind;
    private int device_meal_type;
    private String cardCode;
    private byte[] state;
    private Integer last_money_cash;
    private Integer last_money_corp_grant;
    private Integer money_cash;
    private Integer money_corp_grant;
    private String be_control_time;
    private int mealMoney;
    private int real_mealMoney;
    private int real_meal_cash_Money;//实际扣除现金
    private int real_meal_grant_Money;//实际扣除补贴
    private String pwd_for_beyond_quota;//超额消费密码
    private ServerMessage posServerMessage;
    private String pk_dining_type;
    private String dining_code;
    private String type_name;
    private int serial;
    private String meal_ts;
    private String memo;
    private String tablename;

    public String getTablename() {
        return tablename;
    }

    public void setTablename(String tablename) {
        this.tablename = tablename;
    }

    public String getMemo() {
        return memo;
    }

    public void setMemo(String memo) {
        this.memo = memo;
    }

    public String getMeal_ts() {
        return meal_ts;
    }

    public void setMeal_ts(String meal_ts) {
        this.meal_ts = meal_ts;
    }

    public String getDining_code() {
        return dining_code;
    }

    public void setDining_code(String dining_code) {
        this.dining_code = dining_code;
    }

    public int getSerial() {
        return serial;
    }

    public void setSerial(int serial) {
        this.serial = serial;
    }

    public String getPk_dining_type() {
        return pk_dining_type;
    }

    public void setPk_dining_type(String pk_dining_type) {
        this.pk_dining_type = pk_dining_type;
    }

    public String getType_name() {
        return type_name;
    }

    public void setType_name(String type_name) {
        this.type_name = type_name;
    }

    public String getMeal_kind() {
        return meal_kind;
    }

    public void setMeal_kind(String meal_kind) {
        this.meal_kind = meal_kind;
    }

    public String getDevice_code() {
        return device_code;
    }

    public void setDevice_code(String device_code) {
        this.device_code = device_code;
    }

    public String getPsncode() {
        return psncode;
    }

    public void setPsncode(String psncode) {
        this.psncode = psncode;
    }

    public String getPsnname() {
        return psnname;
    }

    public void setPsnname(String psnname) {
        this.psnname = psnname;
    }

    public String getBe_control_time() {
        return be_control_time;
    }

    public void setBe_control_time(String be_control_time) {
        this.be_control_time = be_control_time;
    }

    public String getCard_state() {
        return card_state;
    }

    public void setCard_state(String card_state) {
        this.card_state = card_state;
    }

    public int getReal_meal_cash_Money() {
        return real_meal_cash_Money;
    }

    public void setReal_meal_cash_Money(int real_meal_cash_Money) {
        this.real_meal_cash_Money = real_meal_cash_Money;
    }

    public int getReal_meal_grant_Money() {
        return real_meal_grant_Money;
    }

    public void setReal_meal_grant_Money(int real_meal_grant_Money) {
        this.real_meal_grant_Money = real_meal_grant_Money;
    }

    public String getPk_corp() {
        return pk_corp;
    }

    public void setPk_corp(String pk_corp) {
        this.pk_corp = pk_corp;
    }

    public int getDevice_meal_type() {
        return device_meal_type;
    }

    public void setDevice_meal_type(int device_meal_type) {
        this.device_meal_type = device_meal_type;
    }

    public int getMeal_type() {
        return meal_type;
    }

    public void setMeal_type(int meal_type) {
        this.meal_type = meal_type;
    }

    public String getPk_card() {
        return pk_card;
    }

    public String getPk_device() {
        return pk_device;
    }

    public void setPk_device(String pk_device) {
        this.pk_device = pk_device;
    }

    public String getPk_meal_rule() { return pk_meal_rule;}

    public void setPk_meal_rule(String pk_meal_rule) { this.pk_meal_rule = pk_meal_rule;}

    public String getPk_psnbasdoc() {
        return pk_psnbasdoc;
    }

    public void setPk_psnbasdoc(String pk_psnbasdoc) {
        this.pk_psnbasdoc = pk_psnbasdoc;
    }

    public void setPk_card(String pk_card) {
        this.pk_card = pk_card;
    }

    public int getReal_mealMoney() {
        return real_mealMoney;
    }

    public void setReal_mealMoney(int real_mealMoney) {
        this.real_mealMoney = real_mealMoney;
    }

    public String getCard_ineffectived_date() {
        return card_ineffectived_date;
    }

    public void setCard_ineffectived_date(String card_ineffectived_date) {
        this.card_ineffectived_date = card_ineffectived_date+" 00:00:00";
    }

    public String getCardCode() {
        return cardCode;
    }

    public void setCardCode(String cardCode) {
        this.cardCode = cardCode;
    }

    public byte[] getState() {
        return state;
    }

    public void setState(byte[] state) {
        this.state = state;
    }

    public Integer getLast_money_cash() {
        return last_money_cash;
    }

    public void setLast_money_cash(Integer last_money_cash) {
        this.last_money_cash = last_money_cash;
    }

    public Integer getLast_money_corp_grant() {
        return last_money_corp_grant;
    }

    public void setLast_money_corp_grant(Integer last_money_corp_grant) {
        this.last_money_corp_grant = last_money_corp_grant;
    }

    public Integer getMoney_cash() {
        return money_cash;
    }

    public void setMoney_cash(Integer money_cash) {
        this.money_cash = money_cash;
    }

    public Integer getMoney_corp_grant() {
        return money_corp_grant;
    }

    public void setMoney_corp_grant(Integer money_corp_grant) {
        this.money_corp_grant = money_corp_grant;
    }

    public int getMealMoney() {
        return mealMoney;
    }

    public void setMealMoney(int mealMoney) {
        this.mealMoney = mealMoney;
    }

    public ServerMessage getPosServerMessage() {
        return posServerMessage;
    }

    public void setPosServerMessage(ServerMessage posServerMessage) {
        this.posServerMessage = posServerMessage;
    }

    public String getPwd_for_beyond_quota() {
        return pwd_for_beyond_quota;
    }

    public void setPwd_for_beyond_quota(String pwd_for_beyond_quota) {
        this.pwd_for_beyond_quota = pwd_for_beyond_quota;
    }
}

