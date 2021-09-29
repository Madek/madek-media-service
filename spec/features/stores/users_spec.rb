require "features/shared/authentication_error"
require "features/shared/authorization_error"

describe "Users", type: :feature do
  let!(:store) { create(:media_store, :database) }
  let(:path) { "/media-service/stores/database/users/" } 

  context "for public" do
    before do
      visit path
    end

    it "doesn't display user's dropdown in navbar" do
      expect(page).not_to have_css('.navbar a.dropdown-toggle')
    end

    it_displays "authentication error"
    it_displays "authorization error"
  end

  context "for an ordinary user" do
    let(:user) { create(:user) }

    before { sign_in }

    it_displays "authorization error"
  end

  context "for an user with admin role" do
    let(:user) { create(:user, :with_admin_role) }

    before { sign_in }

    it_displays "authorization error"
  end

  context "for an user with system admin role" do
    let(:person) { create(:person, first_name: "Richie", last_name: "Horn") }
    let(:user) { create(:user, :with_system_admin_role, person: person) }
    let(:person_2) { create(:person, first_name: "Liana", last_name: "Warner") }
    let(:person_3) { create(:person, first_name: "Clara", last_name: "Warner") }
    let(:person_4) { create(:person, first_name: "Andy", last_name: "Zulu") }
    let!(:user_2) { create(:user, person: person_2, email: "liana@example.com") }
    let!(:user_3) { create(:user, person: person_3, email: "clara@example.com") }
    let!(:user_4) { create(:user, person: person_4) }

    before do
      sign_in

      visit path
    end

    describe "listing" do
      it "displays header" do
        expect(page).to have_css("h2", text: "Media-Store database Users")
      end

      it "displays table with users" do
        expect(page).to have_css("table.users tbody tr", count: 4)
      end

      describe "filtering" do
        it "filters users out" do
          fill_in "term", with: "arn"

          expect(page).to have_css("table.users tbody tr", count: 2)
          within "table.users" do
            expect(page).to have_content "Clara Warner (clara@example.com)"
            expect(page).to have_content "Liana Warner (liana@example.com)"
          end
        end
      end
    end

    describe "user's priority" do
      it "is editable" do
        expect(page).to have_css(".direct-priority-component", text: "-", count: 4)
        expect(page).to have_css(".combined-priority-component", text: "-", count: 4)

        within "tr[data-id='#{user.id}'] .direct-priority-component" do
          click_button "Edit"
        end

        expect(page).to have_css(".modal")
        fill_in "direct_priority", with: 5

        within(".modal") { click_button "Save" }

        expect(page).to have_css('table.users tbody tr', count: 4)
        within "tr[data-id='#{user.id}'] .combined-priority-component" do
          expect(page).to have_content "5"
        end
      end

      it "is deletable" do
        within "tr[data-id='#{user_2.id}'] .direct-priority-component" do
          click_button "Edit"
        end
        within ".modal" do
          fill_in "direct_priority", with: 3
          click_button "Save"
        end

        within "tr[data-id='#{user_2.id}'] .direct-priority-component" do
          click_button "Edit"
        end
        within ".modal" do
          fill_in "direct_priority", with: ""
          click_button "Save"
        end

        within "tr[data-id='#{user_2.id}'] .direct-priority-component" do
          expect(page).to have_content "-"
        end
        within "tr[data-id='#{user_2.id}'] .combined-priority-component" do
          expect(page).to have_content "-"
        end
      end
    end
  end
end
