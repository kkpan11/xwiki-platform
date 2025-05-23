<!--
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
-->


<!--
  DateFilter is a custom filter that allow to filter dates
  It could either evaluate a single date (equals, not equals, before, after, ...)
  or match a date range
-->
<template>
  <div v-if="dependenciesloaded">
    <!-- A simple text input that will be upgraded to have a date picker -->
    <input
      v-if="showDateInput"
      class="filter-date"
      ref="filterDate"
      key="filterDate"
      type="text"
      size="1"
      :aria-label="$t('livedata.filter.date.label')"
      :value="valueFormatted"
    />
    <!-- A simple text input to filter date with text -->
    <input
      v-else
      class="filter-date"
      ref="containsInput"
      key="containsInput"
      type="text"
      size="1"
      :aria-label="$t('livedata.filter.date.label')"
      :value="filterEntry.value"
      @input="applyFilterFromText"
    />
  </div>
</template>


<script>
import filterMixin from "./filterMixin.js";
import { loadById } from "@/services/require.js";

export default {

  name: "filter-date",

  // Add the filterMixin to get access to all the filters methods and computed properties inside this component
  mixins: [filterMixin],

  inject: ["jQuery"],

  data() {
    return {
      rules: [
        {
          from: "contains",
          to: ["before", "after", "between"],
          getValue: () => {
            return "";
          },
        },
        {
          from: ["before", "after", "between"],
          to: "contains",
          getValue: ({ oldValue }) => {
            const date = this.moment((oldValue + "").split("/")[0]);
            return date.isValid() ? date.format(this.format) : oldValue;
          },
        },
        {
          // between x and y => after x and before y
          from: "between",
          to: "before",
          getValue: ({ oldValue }) => {
            return (oldValue + "").split("/")[1] || oldValue;
          },
        },
        {
          from: "between",
          to: "after",
          getValue: ({ oldValue }) => {
            return typeof oldValue === "string" ? oldValue.split("/")[0] : oldValue;
          },
        },
        {
          from: ["before", "after"],
          to: "between",
          getValue: ({ oldValue }) => {
            const date = this.moment(oldValue + "");
            return date.isValid() ? (oldValue + "/" + oldValue) : oldValue;
          },
        },
      ],
      moment: null,
      dependenciesloaded: false,
    };
  },

  computed: {

    valueFormatted() {
      if (typeof this.filterEntry.value === "string" && this.filterEntry.value.length) {
        const range = this.filterEntry.value.split("/");
        if (range.length <= 2) {
          return range.map(dateString => {
            const date = this.moment(dateString);
            return date.isValid() ? date.format(this.format) : dateString;
          }).join(" - ");
        }
      }
      return this.filterEntry.value;
    },

    showDateInput() {
      return this.operator !== "contains";
    },

    format() {
      const javaDateFormat = this.config.dateFormat;
      return javaDateFormat ? this.moment().toMomentFormatString(javaDateFormat) :
        "YYYY/MM/DD HH:mm";
    },

    ranges() {
      return {
        "Today":
          [this.moment().startOf("day"), this.moment().endOf("day")],
        "Yesterday":
          [this.moment().subtract(1, "days").startOf("day"),
            this.moment().subtract(1, "days").endOf("day")],
        "Last 7 days":
          [this.moment().subtract(6, "days").startOf("day"), this.moment().endOf("day")],
        "Last 30 days":
          [this.moment().subtract(29, "days").startOf("day"), this.moment().endOf("day")],
        "This month":
          [this.moment().startOf("month"), this.moment().endOf("month")],
        "Last month":
          [this.moment().subtract(1, "month").startOf("month"),
            this.moment().subtract(1, "month").endOf("month")],
      };
    },

    defaultFilterConfig() {
      return {
        drops: "down",
        opens: "right",
        autoUpdateInput: false,
        autoApply: false,
        singleDatePicker: true,
        timePicker: /[Hhkms]/.test(this.format),
        timePicker24Hour: true,
        showCustomRangeLabel: true,
        alwaysShowCalendars: true,
        locale: {
          format: this.format,
          cancelLabel: "Clear",
        },
      };
    },

    filterConfig() {
      if (this.operator === "between") {
        return {
          ...this.defaultFilterConfig, singleDatePicker: false,
          ranges: this.ranges,
        };
      } else {
        return { ...this.defaultFilterConfig };
      }
    },

  },

  methods: {
    // Get date filter value from input element
    getDateValue() {
      const daterangepicker = this.jQuery(this.$refs.filterDate).data("daterangepicker");
      if (this.operator === "between") {
        // Serialize the date range as a ISO 8601 time interval, without fractional seconds.
        // See https://en.wikipedia.org/wiki/ISO_8601#Time_intervals
        return `${daterangepicker.startDate.format()}/${daterangepicker.endDate.add(59,
          "seconds").format()}`;
      } else if (this.operator === "before" || this.operator === "after") {
        // Use the ISO 8601 representation, without fractional seconds.
        return daterangepicker.startDate.format();
      } else {
        // Use the formatted date.
        return daterangepicker.startDate.format(this.format);
      }
    },

    applyFilterFromDate() {
      const value = this.getDateValue();
      this.applyFilter(value);
    },

    applyFilterFromText() {
      const value = this.jQuery(this.$refs.containsInput).val();
      this.applyFilterWithDelay(value);
    },
  },

  async mounted() {
    this.moment = await loadById("moment");
    await loadById("daterangepicker");
    await loadById("moment-jdateformatparser");
    this.dependenciesloaded = true;
    this.$watch(() => {
        return {
          filterConfig: this.filterConfig,
          loaded: this.dependenciesloaded,
        };
      },
      async ({ loaded }) => {
        if (!loaded) {
          return;
        }
        // make sure the filter date element is rendered.
        await this.$nextTick();
        const filterDate = this.$refs.filterDate;
        // Create the date range picker associated to the single date input
        const $ = this.jQuery;
        $(filterDate).daterangepicker(
          this.filterConfig,
        );
        $(filterDate).on("apply.daterangepicker", () => {
          this.applyFilterFromDate();
        });
        $(filterDate).on("cancel.daterangepicker", () => {
          this.applyFilter("");
        });
        // Fix prototypejs prototype pollution
        $(filterDate).on("hide.daterangepicker", (e) => {
          // Overwrite at instance level the 'hide' function added by Prototype.js to the Element prototype.
          // This removes the 'hide' function only for the event target.
          filterDate.hide = undefined;
          // Restore the 'hide' function after the event is handled (i.e. after all the listeners have been called).
          setTimeout(function() {
            // This deletes the local 'hide' key from the instance, making the 'hide' inherited from the prototype
            // visible again (the next calls to 'hide' won't find the key on the instance and thus it will go up
            // the prototype chain).
            delete e.target["hide"];
          }, 0);
        });
      },
      { immediate: true },
    );
  },

};
</script>


<style>

.livedata-filter .filter-date {
  width: 100%;
  height: 100%;
}

</style>
