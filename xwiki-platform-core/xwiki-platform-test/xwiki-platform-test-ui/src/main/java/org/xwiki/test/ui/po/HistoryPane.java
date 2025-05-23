/*
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
 */
package org.xwiki.test.ui.po;

import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.FindBy;

/**
 * Represents the actions possible on the History Pane at the bottom of a page.
 * 
 * @version $Id$
 * @since 3.2M3
 */
public class HistoryPane extends BaseElement
{
    @FindBy(id = "historycontent")
    private WebElement pane;

    public boolean isOpened()
    {
        return this.pane.isDisplayed();
    }

    public boolean hasVersionWithSummary(String summary)
    {
        List<WebElement> tableEntries = pane.findElements(By.xpath(".//table/tbody/tr"));
        By commentVersionXPath;
        try {
            pane.findElement(By.xpath(".//tr[2]/td/input"));
            commentVersionXPath = By.xpath(".//td[6]");
        } catch (NoSuchElementException e) {
            commentVersionXPath = By.xpath(".//td[4]");
        }
        for (WebElement tableEntry : tableEntries) {
            try {
                WebElement cell = tableEntry.findElement(commentVersionXPath);
                if (cell.getText().trim().contentEquals(summary)) {
                    return true;
                }
            } catch (NoSuchElementException e) {
                // Ignore, better luck next time.
            }
        }
        return false;
    }

    public String getCurrentVersion()
    {
        return getCurrentItemInTable("td[3]/a", "td[1]/a");
    }

    public String getCurrentVersionComment()
    {
        return getCurrentItemInTable("td[6]", "td[4]");
    }

    public String getCurrentAuthor()
    {
        return getCurrentItemInTable("td[4]", "td[2]");
    }

    private String getCurrentItemInTable(String xpathFragment1, String xpathFragment2)
    {
        try {
            // Try to find a radio button. This will mean there are several revisions in the table
            // and we'll find the version written down in the 3rd column
            getDriver().findElementWithoutWaiting(this.pane, By.xpath(".//tr[2]/td/input"));
            return getDriver().findElementWithoutWaiting(this.pane,
                By.xpath(".//*[contains(@class, 'currentversion')]/" + xpathFragment1)).getText();
        } catch (NoSuchElementException e) {
            // If we couldn't find the radio button, there is less columns displayed and the version will be
            // in the first column
            return getDriver().findElementWithoutWaiting(this.pane,
                By.xpath(".//*[contains(@class, 'currentversion')]/" + xpathFragment2)).getText();
        }
    }

    /**
     * IMPORTANT: this method isn't blocking and doesn't wait for the page to be loaded (after the confirmation popup
     *            has been accepted).
     */
    public ViewPage rollbackToVersion(String version)
    {
        getDriver().makeConfirmDialogSilent(true);

        pane.findElement(
            By.xpath(".//table//tr[contains(., '" + version
                + "')]//td[@class='xwikibuttonlink']/a[contains(.,'Rollback')]")).click();

        // A new page is loaded after the dialog is accepted, thus we need to wait that it's loaded before returning
        // as otherwise following actions may be performed on the current page and not on the new page.
        // TODO: Find a generic way to test for this condition. Right now users of this method need to perform their
        // own wait.

        return new ViewPage();
    }

    /**
     * View the document at the given version.
     *
     * @param version the version to view
     * @return the viewpage
     * @since 14.10.7
     * @since 15.2RC1
     */
    public ViewPage viewVersion(String version)
    {
        this.pane.findElement(By.xpath(".//table//tr//td[position()=3]/a[contains(., '" + version + "')]")).click();

        return new ViewPage();
    }

    public HistoryPane deleteVersion(String version)
    {
        getDriver().makeConfirmDialogSilent(true);

        pane.findElement(
            By.xpath(".//table//tr[contains(., '" + version
                + "')]//td[@class='xwikibuttonlink']/a[contains(.,'Delete')]")).click();

        return new HistoryPane();
    }

    /**
     * Clicks on the 'Show Minor Edits' button.
     * 
     * @return the new history pane that includes the minor edits
     */
    public HistoryPane showMinorEdits()
    {
        // We cannot count on Selenium to wait for the page to be reloaded because the action is handled with
        // JavaScript: there's a click event listener that prevents the default behaviour, changes the form action URL
        // and then submits the form (see history.js).
        getDriver().addPageNotYetReloadedMarker();
        getDriver().findElementWithoutWaiting(pane, By.name("viewMinorVersions")).click();
        getDriver().waitUntilPageIsReloaded();
        return new HistoryPane();
    }


    private void selectVersions(String fromVersion, String toVersion)
    {
        String versionXPath = ".//input[@name = 'rev%s' and @value = '%s']";
        getDriver().findElementWithoutWaiting(pane, By.xpath(String.format(versionXPath, 1, fromVersion))).click();
        getDriver().findElementWithoutWaiting(pane, By.xpath(String.format(versionXPath, 2, toVersion))).click();
    }

    public HistoryPane deleteRangeVersions(String fromVersion, String toVersion)
    {
        getDriver().makeConfirmDialogSilent(true);
        this.selectVersions(fromVersion, toVersion);
        getDriver().findElementWithoutWaiting(pane, By.xpath(".//input[@name = 'deleteVersions']")).click();

        return new HistoryPane();
    }

    /**
     * Selects the specified document versions and clicks the button to compare them.
     * 
     * @param fromVersion the from version
     * @param toVersion the to version
     * @return the page that shows the differences between the selected pages
     */
    public ComparePage compare(String fromVersion, String toVersion)
    {
        this.selectVersions(fromVersion, toVersion);
        getDriver().findElementWithoutWaiting(pane, By.xpath(".//input[@accesskey = 'c']")).click();
        return new ComparePage();
    }

    /**
     * @return the total number of versions contained in the history as returned by the live table
     * @since 14.10.17
     * @since 15.5.3
     * @since 15.8RC1
     */
    public int getNumberOfVersions()
    {
        String xpath = ".//div[@class='paginationFilter' and following-sibling::div[@id='historycontent']]";
        WebElement paginationDiv = getDriver().findElementWithoutWaiting(By.xpath(xpath));
        return Integer.parseInt(getDriver().findElementWithoutWaiting(paginationDiv, By.className("totalResultsNo"))
            .getText());
    }

    /**
     * @return {@code true} if the requested version is currently displayed in the history
     * @since 14.10.17
     * @since 15.5.3
     * @since 15.8RC1
     */
    public boolean hasVersion(String version)
    {
        String xpath = String.format(".//table//tr[contains(., '%s')]", version);
        return getDriver().hasElementWithoutWaiting(pane, By.xpath(xpath));
    }
}
