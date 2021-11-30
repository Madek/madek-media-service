require "features/shared/authentication_error"
require "features/shared/authorization_error"

describe "Settings", type: :feature do
  describe "accessibility" do
    let!(:settings) { create(:media_service_setting) }
    let(:path) { "/media-service/settings/" }

    context "for public" do
      before do
        visit path
      end

      it "doesn't display user's dropdown in navbar" do
        expect(page).not_to have_css('.navbar a.dropdown-toggle')
      end

      it "displays empty setting fields" do
        expect(page).to have_field("upload_max_part_size", type: "number", disabled: true, with: "")
        expect(page).to have_field("upload_min_part_size", type: "number", disabled: true, with: "")
      end

      it_displays "authentication error"
      it_displays "authorization error"
    end

    context "for an ordinary user" do
      let(:user) { create(:user) }

      before do
        sign_in

        visit path
      end

      it "displays empty setting fields" do
        expect(page).to have_field("upload_max_part_size", type: "number", disabled: true, with: "")
        expect(page).to have_field("upload_min_part_size", type: "number", disabled: true, with: "")
      end

      it_displays "authorization error"
    end

    context "for an user with admin role" do
      let(:user) { create(:user, :with_admin_role) }

      before do
        sign_in

        visit path
      end

      it "displays empty setting fields" do
        expect(page).to have_field("upload_max_part_size", type: "number", disabled: true, with: "")
        expect(page).to have_field("upload_min_part_size", type: "number", disabled: true, with: "")
      end

      it_displays "authorization error"
    end

    context "for an user with system admin role" do
      let(:user) { create(:user, :with_system_admin_role) }
      let(:default_upload_max_part_size) { 100 * 1024 ** 2 }
      let(:default_upload_min_part_size) { 1024 ** 2 }

      before do
        sign_in

        visit path
      end

      specify "upload_max_part_size field is disabled" do
        expect(page).to have_field("upload_max_part_size",
                                   type: "number",
                                   disabled: true,
                                   with: default_upload_max_part_size)
      end

      specify "upload_min_part_size field is disabled" do
        expect(page).to have_field("upload_min_part_size",
                                   type: "number",
                                   disabled: true,
                                   with: default_upload_min_part_size)
      end

      specify "secret field is disabled and has no value" do
        expect(page).to have_field("secret", type: "text", disabled: true, with: "")
      end

      specify "reset values to default buttons are disabled" do
        expect(page).to have_button("Reset to default", count: 2, disabled: true)
      end

      describe "updating settings" do
        let(:secret) { "PRIVATE_KEY_!@#" }

        it "updates them" do
          click_link "Edit"

          fill_in "upload_max_part_size", with: 12 * 1024 ** 2
          fill_in "upload_min_part_size", with: 10 * 1024 ** 2
          fill_in "secret", with: secret

          click_button "Save"

          expect(page).to have_field("upload_max_part_size",
                                     type: "number",
                                     disabled: true,
                                     with: 12 * 1024 ** 2)
          expect(page).to have_field("upload_min_part_size",
                                     type: "number",
                                     disabled: true,
                                     with: 10 * 1024 ** 2)
          expect(page).to have_field("secret", type: "text", disabled: true, with: secret)
        end
      end

      describe "resetting values to defaults" do
        it "resets the values" do
          click_link "Edit"

          fill_in "upload_max_part_size", with: 2048
          fill_in "upload_min_part_size", with: 16
          click_button "Save"

          click_link "Edit"

          within_input_group_for_input("upload_max_part_size") do
            expect { click_button("Reset to default") }
              .to change(find_field("upload_max_part_size"), :value)
              .from("2048")
              .to(default_upload_max_part_size.to_s)
          end

          within_input_group_for_input("upload_min_part_size") do
            expect { click_button("Reset to default") }
              .to change(find_field("upload_min_part_size"), :value)
              .from("16")
              .to(default_upload_min_part_size.to_s)
          end
        end
      end
    end
  end
end

def within_input_group_for_input(input_id, &block)
  input = find("##{input_id}")
  within(input.first(:xpath, "ancestor::div[@class='input-group']"), &block)
end
