package com.freelocs.theprisons.compat;

import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.item.ItemStack;

import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

public final class ThePrisonsTrinketsCompat {
    private static final boolean AVAILABLE;
    private static final Method GET_TRINKET_COMPONENT;
    private static final Method GET_EQUIPPED;

    static {
        Method getTrinketComponent = null;
        Method getEquipped = null;
        boolean available = false;

        try {
            Class<?> trinketsApi = Class.forName("dev.emi.trinkets.api.TrinketsApi");
            Class<?> trinketComponent = Class.forName("dev.emi.trinkets.api.TrinketComponent");
            getTrinketComponent = trinketsApi.getMethod("getTrinketComponent", net.minecraft.entity.LivingEntity.class);
            getEquipped = trinketComponent.getMethod("getEquipped", Predicate.class);
            available = true;
        } catch (ReflectiveOperationException exception) {
            available = false;
        }

        AVAILABLE = available;
        GET_TRINKET_COMPONENT = getTrinketComponent;
        GET_EQUIPPED = getEquipped;
    }

    private ThePrisonsTrinketsCompat() {
    }

    public static boolean isAvailable() {
        return AVAILABLE;
    }

    public static List<ItemStack> getEquippedStacks(ClientPlayerEntity player) {
        if (!AVAILABLE || player == null) {
            return Collections.emptyList();
        }

        try {
            Object optionalValue = GET_TRINKET_COMPONENT.invoke(null, player);
            if (!(optionalValue instanceof Optional<?> optional) || optional.isEmpty()) {
                return Collections.emptyList();
            }

            Object component = optional.get();
            Object equipped = GET_EQUIPPED.invoke(component, (Predicate<Object>) value -> true);
            return extractStacks(equipped);
        } catch (ReflectiveOperationException exception) {
            return Collections.emptyList();
        }
    }

    private static List<ItemStack> extractStacks(Object value) {
        List<ItemStack> stacks = new ArrayList<>();
        if (value == null) {
            return stacks;
        }

        if (value instanceof Map<?, ?> map) {
            for (Object entryValue : map.values()) {
                stacks.addAll(extractStacks(entryValue));
            }
            return stacks;
        }

        if (value instanceof Iterable<?> iterable) {
            for (Object entry : iterable) {
                ItemStack stack = extractStack(entry);
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
            return stacks;
        }

        if (value.getClass().isArray()) {
            int length = Array.getLength(value);
            for (int index = 0; index < length; index++) {
                ItemStack stack = extractStack(Array.get(value, index));
                if (!stack.isEmpty()) {
                    stacks.add(stack);
                }
            }
        }

        return stacks;
    }

    private static ItemStack extractStack(Object entry) {
        if (entry instanceof ItemStack stack) {
            return stack;
        }

        if (entry == null) {
            return ItemStack.EMPTY;
        }

        for (String methodName : new String[] {"getRight", "right", "getSecond", "second"}) {
            try {
                Method method = entry.getClass().getMethod(methodName);
                Object value = method.invoke(entry);
                if (value instanceof ItemStack stack) {
                    return stack;
                }
            } catch (ReflectiveOperationException ignored) {
            }
        }

        if (entry.getClass().isArray() && Array.getLength(entry) > 1) {
            Object value = Array.get(entry, 1);
            if (value instanceof ItemStack stack) {
                return stack;
            }
        }

        return ItemStack.EMPTY;
    }
}
