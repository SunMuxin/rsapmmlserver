package com.neusoft.aclome.alert.ai.lib.util;

public class CONSTANT {
	
	public static final String Job_Data_Map_Info = "info";
	public static final String Job_Data_Map_Filters = "filters";

	public static final String RESOURES_TYPE_JAVAEE = "JAVAEE_JAVAEE";
	public static final String THREAD_DIAGNOSE_URL_KEY = "Thread_Diagnose_URL";
	public static final String THREAD_DIAGNOSE_METRIC = "JAVAEE_CPU_used";
	public static final String SYSTEM_INFOMATION_URL_KEY = "System_Info_URL";
	public static final String THREAD_CATEGORY_NAME = "THREAD";
	public static final String LOCK_CATEGORY_NAME = "LOCK";
	
	public static final int int_zero = 0;
	public static final int int_one = 1;
	public static final int int_two = 2;
	public static final int int_three = 3;
	
	public static final double double_zero = 0;
	public static final double double_one = 1;
	public static final double double_one_hundred = 100;

	public static final double get_thread_lock_threshold = 80.0;
	
	public static final double anomaly_threshold = 0.75;
	public static final double top_thread_threshold = 0.55;
	public static final double http_thread_threshold = 0.65;
	public static final double jdbc_thread_threshold = 0.65;
	public static final double gc_thread_threshold = 0.15;
	public static final double lower_cpu_usage_threshold = 0.13;
	public static final int cpu_context_switch_threshold = 50000;
	public static final int thread_lock_chain_threshold = 6;
	public static final int top_thread_count = 5;
	public static final int key_chain_limit_length = int_three;
	
	public static final long two_minite = 1000 * 60 * 2;
	public static final long ten_second = 1000 * 10;
	
	public static final String RCA_RESULT_KEY = "result";
	public static final String RCA_RES_ID_KEY = "res_id";
	public static final String RCA_RES_TYPE_KEY = "res_type";
	public static final String RCA_RES_NAME_KEY = "name";
	public static final String RCA_VALUE_KEY = "value";
	public static final String RCA_THREAD_STACK_TRACE_KEY = "thread_stack_trace";
	public static final String RCA_THREAD_DURATION_KEY = "thread_duration";
	public static final String RCA_THREAD_STATE_KEY = "thread_state";
	public static final String RCA_THREAD_LOCK_KEY = "lock_thread";
	public static final String RCA_TYPE_KEY = "type";
	public static final String RCA_STATS_FILEDS_KEY = "stats_fields";
	public static final String RCA_ALERT_POLICY_ID_KEY = "alert_policy_id";
	public static final String RCA_ALERT_POLICY_NAME_KEY = "alert_policy_name";
	public static final String RCA_MEASUREENT_ID_KEY = "measurement_id";
	public static final String RCA_METRIC_NAME_KEY = "metric_name";
	public static final String RCA_PRIORITY_KEY = "priority";
	public static final String RCA_SYSTEM_INFOMATION_KEY = "system_info";
	public static final String RCA_THREAD_SUMMARY_KEY = "thread_summary";
	
	public static final String RCA_THREAD_DIAGNOSE_RESULT_VALUE = "jm_thread_ad";
	public static final String RCA_ANOMALY_DETECTION_RESULT_VALUE = "ad";

	public static final String OPTION_INTERVAL_KEY = "interval_l";
	public static final String OPTION_SOLR_WRITER_URL_KEY = "solr_writer_url_s";
	public static final String OPTION_SOLR_READER_URL_KEY = "solr_reader_url_s";
	public static final String OPTION_RES_ID_KEY = "res_id_s";
	public static final String OPTION_RES_NAME_KEY = "name_s";
	public static final String OPTION_OPTION_KEY = "option_s";
	public static final String OPTION_RES_APP_NAME_KEY = "appName_s";
	public static final String OPTION_RES_IP_KEY = "ip_s";
	public static final String OPTION_RES_PORT_KEY = "port_s";
	public static final String OPTION_RES_TYPE_KEY = "res_type_s";
	public static final String OPTION_STATS_FIELD_KEY = "stats_field_s";
	public static final String OPTION_STATS_FACET_KEY = "stats_facet_s";
	public static final String OPTION_STATS_TYPE_KEY = "stats_type_s";
	public static final String OPTION_MAX_KEY = "max_f";
	public static final String OPTION_MIN_KEY = "min_f";
	public static final String OPTION_ALERT_MEASUREENT_ID_KEY = "measurement_id_s";
	public static final String OPTION_ALERT_POLICY_ID_KEY = "alert_policy_id_s";
	public static final String OPTION_ALERT_POLICY_NAME_KEY = "alert_policy_name_s";
	public static final String OPTION_ALERT_METRIC_NAME_KEY = "metricName_s";
	public static final String OPTION_ALERT_PRIORITY_KEY = "priority_i";
	
	public static final String OPTION_THREAD_DIAGNOSE_RESULT_VALUE = "jm_thread_ad";
	public static final String OPTION_ANOMALY_DETECTION_RESULT_VALUE = "ad";
	
	public static final String DATA_BASIC_FQ = "one_level_type:basic_info";

	public static final String SOLR_STATS_TYPE_MEAN = "mean";
	public static final String SOLR_ID_KEY = "id";
	
	public static final String DATA_RES_ID_KEY = "res_id";
	
	public static final String MESSAGE_ANOMALY = "anomaly";
	public static final String MESSAGE_NOMALY = "nomaly";
	
	public static final String HTTP_HEADER = "http://";
	public static final char between_ip_and_port = ':';
	public static final char net_spliter = '/';
	public static final String MONITORING_PART_THREAD_LOCK = "monitoring?part=threadlock&format=json";
	public static final String MONITORING_PART_SYSTEM_INFOMATION = "monitoring?part=systeminformation&format=json";

	public static final char at = '@';
	public static final char colon = ':';
	public static final char and = '&';
	public static final char asterisk = '*';
	public static final char period = '.';
	public static final char comma = '，';
	public static final char newline = '\n';
	public static final char spacing = ' ';
	
	public static final String ENCODING_GBK = "GBK";
	
	public static final String STRING_WU = "无";
	public static final String STRING_RUNNABLE = "RUNNABLE";
	public static final String STRING_ABSENCE = "ABSENCE";
	public static final String STRING_NAME = "name";
	public static final String STRING_TIME = "time";
	public static final String STRING_STATE = "state";
	public static final String STRING_STACK_TRACE = "stack_trace";
	public static final String STRING_NULL = "";
	public static final String STRING_LONGEST_THREAD_LOCK_CHAIN = "线程和线程锁的最长路径";
	public static final String STRING_CPU_CONTEXT_SWITCH_COUNTER = "线程状态切换次数";
	public static final String STRING_CPU_CONSUMPTION_SCATTER = "CPU资源消耗分散到不同线程中";
	public static final String STRING_HTTP_THREAD_CPU_CONSUMPTION = "HTTP线程占用CPU";
	public static final String STRING_JDBC_THREAD_CPU_CONSUMPTION = "JDBC线程占用CPU";
	public static final String STRING_GC_THREAD_CPU_CONSUMPTION = "GC线程占用CPU";
	public static final String STRING_CPU_CONSUMPTION_FOCUS = "CPU资源消耗分散到不同线程中";
	public static final String STRING_THREAD_CPU_CONSUMPTION = "具体线程CPU消耗信息详见：线程运行时间";
	public static final String STRING_PROCESS_CPU_CONSUMPTION_TREND = "应用进程CPU消耗趋势详见：线程整体CPU使用率";
	public static final String STRING_HTTP_THREAD_CHECK = "查看HTTP线程执行代码段，请在“线程调用栈（采样）”的检索框内输入:http";
	public static final String STRING_JDBC_THREAD_CHECK = "查看JDBC线程执行代码段，请在“线程调用栈（采样）”的检索框内输入:jdbc";
	public static final String STRING_GC_THREAD_CHECK = "查看HTTP线程执行代码段，GC线程是C线程不存在JAVA线程栈，请在“线程调用栈（采样）”的检索框内输入:GC Daemon";
	public static final String STRING_THREAD_LOCK_CHECK = "最长线程锁请求详见：线程锁请求链";
	public static final String STRING_OPTIMIZE_TOP_THREAD = "可以优化消耗CPU资源较多的线程";
	public static final String STRING_OPTIMIZE_THREAD_LOCK = "可以优化线程锁策略，避免死锁和线程状态切换过于频繁消耗资源";
	public static final String STRING_OPTIMIZE_HTTP_THREAD = "可以优化消耗CPU资源较多的HTTP执行线程，或者对HTTP请求进行分流";
	public static final String STRING_OPTIMIZE_JDBC_THREAD = "可以优化消耗CPU资源较多的JDBC执行线程，或者对相应字段建立索引提高检索效率";
	public static final String STRING_OPTIMIZE_GC_THREAD = "可以优化JVM的新生代和老年代比例，或者增加内存";
	public static final String STRING_OPTIMIZE_LOWER_CPU_USAGE = "请确认关键线程是否执行，或者出现访问问题";

	public static final String STRING_FORMAT_SOLR_SECTION_FQ = "%s:[%s TO %s]";
	public static final String STRING_FORMAT_SOLR_BASIC_FQ = "%s:%s";

	public static final String SUMMARY_THREAD_COUNT = "thread_count";
	public static final String SUMMARY_DEAD_LOCK = "dead_lock";
	public static final String SUMMARY_LOCK_COUNT = "lock_count";
	public static final String SUMMARY_LONGEST_LOCK_CHAIN = "longest_lock_chain";
	public static final String SUMMARY_CPU_CONTEXT_SWITCH = "cpu_context_switch";
	public static final String SUMMARY_GC_TIME = "gc_time";
	public static final String SUMMARY_THREAD_SUGGESTION = "thread_suggestion";
	public static final String SUMMARY_THREAD_SUMMARY = "thread_summary";
	
	public static final String INFO_RES_ID_KEY = "id";
	public static final String INFO_RES_NAME_KEY = "name";
	public static final String INFO_RES_APP_NAME_KEY = "appName";
	public static final String INFO_RES_IP_KEY = "ip";
	public static final String INFO_RES_PORT_KEY = "port";
	public static final String INFO_RES_TYPE_KEY = "type";

	public static final String DB_THREAD_INCLUDE_STRING = "jdbc";
	public static final String HTTP_THREAD_INCLUDE_STRING = "http";
	public static final String GC_THREAD_INCLUDE_STRING = "GC Daemon";

}
