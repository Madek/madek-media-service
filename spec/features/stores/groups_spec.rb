require "features/shared/authentication_error"
require "features/shared/authorization_error"

describe "Groups", type: :feature do
  let!(:store) { create(:media_store, :database) }
  let(:path) { "/media-service/stores/database/groups/" }

  context "for public" do
    before do
      visit path
    end

    it "doesn't display user's dropdown in navbar" do
      expect(page).not_to have_css('.navbar a.dropdown-toggle')
    end

    it_displays "authentication error"
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
    let(:user) { create(:user, :with_system_admin_role) }
    let!(:group_1) { create(:group, name: "Empty group") }
    let!(:group_2) { create(:group, :with_user, name: "Masters") }
    let!(:group_3) { create(:group, name: "Family") }
    let!(:institutional_group) do
      create(:institutional_group,
              name: "Foo Institutional Group",
              institutional_name: "FOO.InstitutionalGroup")
    end

    before do
      group_3.users << create_list(:user, 3)
      store.groups << [group_1, group_2]
    end

    before do
      sign_in

      visit path
    end

    describe "listing" do
      it "displays header" do
        expect(page).to have_css("h2", text: "Media-Store database Groups")
      end

      it "displays table with groups" do
        expect(page).to have_css("table.groups tbody tr", count: 4)

        expect_order(group_1, group_3, institutional_group, group_2)

        expect_row(group_1, priority: 0, users_count: 0)
        expect_row(group_3, priority: "-", users_count: 3)
        expect_row(institutional_group, priority: "-", users_count: 0)
        expect_row(group_2, priority: 0, users_count: 1)
      end

      describe "filtering by user" do
        let(:path) { "/media-service/stores/database/groups/?including-user=#{user.id}" }

        before do
          group_1.users << user
          institutional_group.users << user
        end

        it "filters groups out" do
          expect(page).to have_css("table.groups tbody tr", count: 2)

          expect_order(group_1, institutional_group)

          expect_row(group_1, priority: 0, users_count: 1)
          expect_row(institutional_group, priority: "-", users_count: 1)
        end
      end
    end

    describe "groups's priority" do
      it "is editable" do
        expect_row(group_1, priority: 0, users_count: 0)
        expect(page).not_to have_css(".modal")

        within "tr[data-id='#{group_1.id}'] .priority-component" do
          click_button "Edit"
        end

        within ".modal" do
          fill_in "priority", with: 5
          click_button "Save"
        end

        expect_row(group_1, priority: 5, users_count: 0)
        expect(page).not_to have_css(".modal")
      end

      it "is deletable" do
        expect(page).not_to have_css(".modal")

        within "tr[data-id='#{group_3.id}'] .priority-component" do
          click_button "Edit"
        end
        within ".modal" do
          fill_in "priority", with: 3
          click_button "Save"
        end

        within "tr[data-id='#{group_3.id}'] .priority-component" do
          click_button "Edit"
        end
        within ".modal" do
          fill_in "priority", with: ""
          click_button "Save"
        end

        expect_row(group_3, priority: "-", users_count: 3)
        expect(page).not_to have_css(".modal")
      end
    end
  end
end

def collect_data_ids
  all("tr[data-id]").map { |tr| tr[:"data-id"] }
end

def expect_order(*groups)
  expect(collect_data_ids).to eq(groups.map(&:id))
end

def expect_row(group, priority:, users_count:)
  within "tr[data-id='#{group.id}']" do
    expect(page).to have_content(group.name)
    expect(page).to have_css(".priority-component", text: priority)
    expect(page).to have_css(".users-count", text: users_count)
  end
end
