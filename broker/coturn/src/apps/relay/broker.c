/**
 ******************************************************************************
 * @b Project : reThink
 *
 * @b Sub-project : QoS Broker
 *
 ******************************************************************************
 *
 *                       Copyright (C) 2016 Orange Labs
 *                       Orange Labs - PROPRIETARY
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. *
 ******************************************************************************
 *
 * @brief Extended features drawn for the broker project
 *
 * @file
 *
 */

#include <sys/types.h>
#include <sys/sysinfo.h>

#include "broker.h"

#if defined(TURN_NO_HIREDIS)
#error "BROKER can't be used without HIREDIS. Please install hiredis package and run again configure"
#endif

broker_ctx_t    broker_ctx ;


void setup_db_connection(broker_ctx_t * p_broker_ctx) ;
void register_to_db(const broker_ctx_t * p_broker_ctx) ;
void turn_register_timeout_handler(ioa_engine_handle e, void * arg) ;
static void * run_broker_server_thread(void * arg) ;
static void run_events(struct event_base *eb, ioa_engine_handle e) ;

double getCpuUsage(void) ;
double getPhysMemUsage(void) ;
double getSwapMemUsage(void) ;

//
//
//
void setup_broker_server(void)
{
    const char * db_cnx_string = turn_params.redis_brokerdb ;

    TURN_LOG_FUNC(TURN_LOG_LEVEL_INFO, "Setup broker server\n") ;

    /*
     * If no Broker DB has been set, we can't go further on
     */
    if( db_cnx_string && strlen(db_cnx_string) == 0 )
    {
        TURN_LOG_FUNC(TURN_LOG_LEVEL_ERROR, "[%s:%d] %s - Empty database connection string\n", __FILE__, __LINE__, __FUNCTION__) ;
        TURN_LOG_FUNC(TURN_LOG_LEVEL_ERROR, "[%s:%d] %s - Abnormal. Broker interface is deactivated\n", __FILE__, __LINE__, __FUNCTION__) ;
        return ;
    }

    /*
     * If public_mapped_ip has not be defined by the user, try to use the first relay on which
     * the server is listening.
     * The choice of the relay (and not listener) is because relay is never set with local IP
     * The choice of the first one is arbitrary (usually the IP V4)
     */
    if( strlen(turn_params.public_mapped_ip) == 0 )
    {
        // Insure the pointers are not null
        if( turn_params.relay_addrs && turn_params.relay_addrs[0] )
            strncpy(turn_params.public_mapped_ip, turn_params.relay_addrs[0], sizeof(turn_params.public_mapped_ip)) ;
        else
        {
            TURN_LOG_FUNC(TURN_LOG_LEVEL_ERROR, "[%s:%d] %s - public_mapped_ip not specified and can't set it auto\n", __FILE__, __LINE__, __FUNCTION__) ;
            return ;
        }
    }

    /*
     * If public_mapped_ip has not be defined by the user, use the listener-port
     */
    if( turn_params.public_mapped_port == 0 )
        turn_params.public_mapped_port = turn_params.listener_port ;

    // Build the register key
    snprintf(broker_ctx.db_turn_register_key, sizeof(broker_ctx.db_turn_register_key) , "turnservers:%s:%d", turn_params.public_mapped_ip, turn_params.public_mapped_port) ;
    TURN_LOG_FUNC(TURN_LOG_LEVEL_INFO, "Registered key used : %s\n", broker_ctx.db_turn_register_key) ;

    setup_db_connection(&broker_ctx) ;

    if( pthread_create(&broker_ctx.thr, NULL, run_broker_server_thread, &broker_ctx) )
    {
        TURN_LOG_FUNC(TURN_LOG_LEVEL_ERROR, "[%s:%d] %s - Can't create broker thread\n", __FILE__, __LINE__, __FUNCTION__) ;
        return ;
    }

    /*
     *  I'm not sure to understand what's the real need to do that but all
     *  Turn threads are detached, so I also do it
     */
    pthread_detach(broker_ctx.thr) ;

    // Now register the turn server to the DB
    register_to_db(&broker_ctx) ;

}// setup_broker_server


/**
 * Initialize the database connection context
 *
 * @param[in] p_broker_ctx  : Broker context pointer
 * @param[in] db_cnx_string : The (redis) DB cnx string
 *
 * @return N/A
 */
void setup_db_connection(broker_ctx_t * p_broker_ctx)
{
    /*
     * This is not a "real" engine but I need this kind of instance to use
     * the ioa_timer features.
     */
    static ioa_engine faked_engine ;

    p_broker_ctx->event_base = turn_event_base_new() ;

    // This is wanted by the function set_ioa_timer
    faked_engine.event_base = p_broker_ctx->event_base ;

    /*
     * BE CAREFUL, I try to avoid some branch code inside this below function,
     * that's why the 3rd parameter MUST NOT be equal to zero (number of milliseconds)
     */
    set_ioa_timer(&faked_engine, TURN_REGISTER_REFRESH_PERIOD, 1, turn_register_timeout_handler, p_broker_ctx, 1, "turn_register_timeout_handler");

    // setup the redis connection
    p_broker_ctx->rch = get_redis_async_connection(p_broker_ctx->event_base, turn_params.redis_brokerdb, 0) ;

}// setup_db_connection


/**
 * Handler of the TTL timeout registered turn key
 *
 * @see setup_db_connection
 *
 * @return N/A
 */
void turn_register_timeout_handler(ioa_engine_handle e, void * arg)
{
    UNUSED_ARG(e) ;

    register_to_db(arg) ;

}// turn_register_timeout_handler


/**
 * Register (ie refresh) the current Turn server into DB
 *
 * @param[in] p_broker_ctx : broker context pointer
 *
 * @see setup_db_connection
 *
 * @return N/A
 */
void register_to_db(const broker_ctx_t * p_broker_ctx)
{
    // char key[] = "truc" ;

    // send_message_to_redis(p_broker_ctx->rch, "set", key, "tata=%s", "truc tout pourri") ;
    // send_message_to_redis(p_broker_ctx->rch, "expire", "truc", "%d", TURN_REGISTERED_TTL_VALUE) ;

    send_message_to_redis(  p_broker_ctx->rch,
                            "set",
                            broker_ctx.db_turn_register_key,
                            "cpu=%.2lf%%, phys_mem=%.2lf%%, swap_mem=%.2lf%%",
                            getCpuUsage(),
                            getPhysMemUsage(),
                            getSwapMemUsage()) ;

    send_message_to_redis(p_broker_ctx->rch, "expire", broker_ctx.db_turn_register_key, "%d", TURN_REGISTERED_TTL_VALUE) ;

    TURN_LOG_FUNC(TURN_LOG_LEVEL_INFO, "Broker register/refresh into database\n") ;


}// register_to_db


/**
 * Retrieve the global CPU usage
 *
 * @return Percent of CPU usage
 */
double getCpuUsage(void)
{
    double  percent ;
    FILE *    file ;
    static unsigned long long lastTotalUser, lastTotalUserLow, lastTotalSys, lastTotalIdle ;
    static bool first_call = true ;

    unsigned long long totalUser, totalUserLow, totalSys, totalIdle, total;

    // When the first call occurs, initialize the first values of lastXxxxx. Then
    // the algorithm roll them automatically
    if( first_call )
    {
        first_call = false ;
        file = fopen("/proc/stat", "r") ;
        fscanf(file, "cpu %llu %llu %llu %llu", &lastTotalUser, &lastTotalUserLow, &lastTotalSys, &lastTotalIdle);
        fclose(file) ;
    }

    file = fopen("/proc/stat", "r") ;
    fscanf(file, "cpu %llu %llu %llu %llu", &totalUser, &totalUserLow, &totalSys, &totalIdle) ;
    fclose(file);

    if (totalUser < lastTotalUser || totalUserLow < lastTotalUserLow ||
        totalSys < lastTotalSys || totalIdle < lastTotalIdle)
    {
        //Overflow detection. Just skip this value.
        percent = -1.0;
    }
    else
    {
        total = (totalUser - lastTotalUser) + (totalUserLow - lastTotalUserLow) +
            (totalSys - lastTotalSys);
        percent = total;
        total += (totalIdle - lastTotalIdle);
        // Prevent division by zero
        percent = (total != 0 ? percent * 100 / total : 0) ;
    }

    // Save (Roll) the values
    lastTotalUser = totalUser;
    lastTotalUserLow = totalUserLow;
    lastTotalSys = totalSys;
    lastTotalIdle = totalIdle;

    return percent;

}// getCpuUsage


/**
 * Retrieve the physical memory usage
 *
 * @return Percent of memory
 */
double getPhysMemUsage(void)
{
    double percent ;
    struct sysinfo memInfo ;

    sysinfo (&memInfo) ;

    long long physMemUsed = memInfo.totalram - memInfo.freeram;

    // physMemUsed *= memInfo.mem_unit;

    percent = physMemUsed * 100 / memInfo.totalram ;

    return percent ;

}// getPhysMemUsage


/**
 * Retrieve the swap memory usage
 *
 * @return Percent of memory
 */
double getSwapMemUsage(void)
{
    double percent ;
    struct sysinfo memInfo ;

    sysinfo (&memInfo) ;

    long long swapMemUsed = memInfo.totalswap - memInfo.freeswap ;

    // swapMemUsed *= memInfo.mem_unit;

    percent = swapMemUsed * 100 / memInfo.totalswap ;

    return percent ;

}// getSwapMemUsage




/**
 * Main thread of the broker
 *
 * @param[in] arg : Broker context pointer
 *
 * @return N/A
 */
static void * run_broker_server_thread(void * arg)
{
    broker_ctx_t * p_broker_ctx = (broker_ctx_t *)arg ;

    ignore_sigpipe();

    /*
     * I may adjust the barrier level to synchronize all threads before
     * using this call. It seems not to be mandatory up to now but I let
     * this call in comment to identify what should be needed
     */
    // barrier_wait();

    while( p_broker_ctx->event_base )
    {
        run_events(p_broker_ctx->event_base, NULL) ;
    }

    return arg ;
}


/**
 * Event dispatcher
 * copied/pasted from netengine.c
 *
 * @return N/A
 */
static void run_events(struct event_base *eb, ioa_engine_handle e)
{
    if(!eb && e)
        eb = e->event_base;

    if (!eb)
        return;

    struct timeval timeout;

    timeout.tv_sec = 5;
    timeout.tv_usec = 0;

    event_base_loopexit(eb, &timeout);

    event_base_dispatch(eb);

}// run_events

