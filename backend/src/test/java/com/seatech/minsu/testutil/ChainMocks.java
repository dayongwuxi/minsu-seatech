package com.seatech.minsu.testutil;

import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

/**
 * MyBatis-Plus 链式 wrapper 的测试桩：泛型桥方法导致 RETURNS_SELF 失效，
 * 统一在此显式打桩常用链方法（返回自身），终结方法(one/list/count/update)由用例自行 stub。
 */
public final class ChainMocks {

    @SuppressWarnings("unchecked")
    public static <T> LambdaQueryChainWrapper<T> queryChain() {
        LambdaQueryChainWrapper<T> chain = mock(LambdaQueryChainWrapper.class);
        lenient().when(chain.eq(any(), any())).thenReturn(chain);
        lenient().when(chain.lt(any(), any())).thenReturn(chain);
        lenient().when(chain.gt(any(), any())).thenReturn(chain);
        lenient().when(chain.in(any(), any(Object[].class))).thenReturn(chain);
        lenient().when(chain.isNotNull(any())).thenReturn(chain);
        lenient().when(chain.orderByDesc(ChainMocks.<T>anyColumn())).thenReturn(chain);
        lenient().when(chain.last(any())).thenReturn(chain);
        return chain;
    }

    @SuppressWarnings("unchecked")
    public static <T> LambdaUpdateChainWrapper<T> updateChain() {
        LambdaUpdateChainWrapper<T> chain = mock(LambdaUpdateChainWrapper.class);
        lenient().when(chain.eq(any(), any())).thenReturn(chain);
        lenient().when(chain.set(any(), any())).thenReturn(chain);
        lenient().when(chain.update()).thenReturn(true);
        return chain;
    }

    private static <T> SFunction<T, ?> anyColumn() {
        return any();
    }

    private ChainMocks() {
    }
}
